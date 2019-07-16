package com.secure.keychain;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.google.firebase.iid.FirebaseInstanceId;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

public class RNSecureKeychainModule extends ReactContextBaseJavaModule
{
	public static final String TAG = "EX:KeychainModule";
	private static final boolean D = BuildConfig.DEBUG;

	private final ReactApplicationContext reactContext;
	private static String s_psw = "";
	private static String ALG = "AES";
	private static ExecutorService s_executor = Executors.newSingleThreadExecutor();

	private static String E_CRYPTO = "crypto-error";
	private static String E_IO = "io-error";

	private SharedPreferences preferences;
	private static String keychainDir = "keychain/";

	static {
		System.loadLibrary("Secure-keychain");
	}

	//region REACT NATIVE
	public RNSecureKeychainModule(ReactApplicationContext reactContext)
	{
		super(reactContext);
		this.reactContext = reactContext;
		this.preferences = reactContext.getSharedPreferences("Secure-keychain-preferences", Context.MODE_PRIVATE);
	}

	@Override
	public String getName()
	{
		return "RNSecureKeychain";
	}

	@ReactMethod
	public void unlock(final Promise promise)
	{
		s_executor.submit(new Runnable() {
			@Override
			public void run() {
				s_psw = fmd5( FirebaseInstanceId.getInstance().getId() );

				if(!preferences.getBoolean("migrated_fmd5", false))
				{
					if( D ) Log.d(TAG, "Migration of keychain to fmd5");

					keychainDir = "";
					migrateFile(reactContext, "/", FirebaseInstanceId.getInstance().getId(), s_psw);

					keychainDir = "keychain/";
					preferences
							.edit()
							.putBoolean("migrated_fmd5", true)
							.apply();

					if( D ) Log.d(TAG, "Migration finished");
				}

				promise.resolve(true);
			}
		});
	}

	@ReactMethod
	public void load(final String path, final Promise promise)
	{
		s_executor.submit(new Runnable() {
			@Override
			public void run() {
				try {
					promise.resolve( new String( load(reactContext, path) ) );
				} catch (IOException e) {
					promise.reject(E_IO, e);
				} catch (NoSuchAlgorithmException | BadPaddingException | IllegalBlockSizeException |
						NoSuchPaddingException | InvalidKeyException e) {
				    promise.reject(E_CRYPTO, e);
				}
			}
		});
	}

	@ReactMethod
	public void save(final String path, final String data, final Promise promise)
	{
		s_executor.submit(new Runnable() {
			@Override
			public void run() {
				try {
					boolean ret = save( reactContext, path, data.getBytes() );
					promise.resolve(ret);
				} catch (IOException e) {
					promise.reject(E_IO, e);
				} catch (NoSuchAlgorithmException | BadPaddingException | IllegalBlockSizeException |
						NoSuchPaddingException | InvalidKeyException e) {
				    promise.reject(E_CRYPTO, e);
				}
			}
		});
	}

	@ReactMethod
	public void remove(final String path, final Promise promise)
	{
		s_executor.submit(new Runnable() {
			@Override
			public void run() {

				if( remove( reactContext, path ) )
					promise.resolve(true);
				else
					promise.resolve(false);
			}
		});
	}
	//endregion

	public static String loadString(Context context, String path){

		try {
			return new String( load(context,path) );
		} catch (IOException | NoSuchAlgorithmException | IllegalBlockSizeException | InvalidKeyException | BadPaddingException | NoSuchPaddingException e) {
			return "";
		}
	}

	public static boolean saveString(Context context, String path, String data){

		try {
			return save(context, path, data.getBytes());
		} catch (IOException | NoSuchAlgorithmException | IllegalBlockSizeException | InvalidKeyException | BadPaddingException | NoSuchPaddingException e) {
			return false;
		}
	}

	public static boolean save(Context context, String path, byte[] data) throws NoSuchAlgorithmException, IllegalBlockSizeException,
			InvalidKeyException, BadPaddingException, NoSuchPaddingException, IOException {

		if( s_psw.equals("") )
			return false;

		return saveFile(context, keychainDir + path, encrypt(sha256(s_psw), data));
	}

	public static byte[] load(Context context, String path) throws IOException, NoSuchAlgorithmException, IllegalBlockSizeException,
			InvalidKeyException, BadPaddingException, NoSuchPaddingException {

		if( s_psw.equals("") )
			return "".getBytes();

		return decrypt(sha256(s_psw), loadFile(context, keychainDir + path));
	}

	public static boolean remove(Context context, String path){

		if( s_psw.equals("") )
			return false;

		return removeFile(context, keychainDir + path);
	}

	public static boolean exists(Context context, String path) {
		String appPath = context.getFilesDir().getPath();
		String filePath = appPath + "/" + keychainDir + path;
		File file = new File(filePath);

		return file.exists();
	}

	private static void migrateFile(Context context, String relativePath, String oldPass, String newPass){
		String rootPath = context.getFilesDir().getPath();

		relativePath = relativePath.charAt(0) == '/' ? relativePath.substring(1) : relativePath;

		String fullPath = rootPath + "/" + relativePath;
		File issuedFile = new File(fullPath);

		String[] whiteList = { "pin.hash","companies","pin_verifications" };

		if(issuedFile.isHidden()){
			if( D ) Log.d(TAG, "migrateFile(): hidden file "+relativePath+" ignored");
		}
		else if(issuedFile.isDirectory() && !issuedFile.isHidden()){
			File[] files = issuedFile.listFiles();

			for(File file : files){
				String filePath = relativePath + "/" + file.getName();
				migrateFile(context, filePath, oldPass, newPass);
			}
		}
		else if(issuedFile.isFile()){

			if( !relativePath.contains("/") ){

				if( Arrays.asList(whiteList).contains(relativePath) ) {
					Log.i(TAG, "migrate (whitelisted) file: " + relativePath);
					if( D ) Log.d(TAG, "migrateFile(): whitelisted file "+relativePath);
				}
				else if( relativePath.contains("Secure-") ){
					if( D ) Log.d(TAG, "migrateFile(): keystore fallback file "+relativePath);
				} else {
					return;
				}
			}

			if( D ) Log.d(TAG, "migrateFile(): doing migration of file "+relativePath);

			try
			{
				s_psw = oldPass;
				byte[] data = load( context, relativePath );

				s_psw = newPass;
				save( context, "keychain/" + relativePath, data );

				removeFile( context, relativePath );
			}
			catch (IOException | NoSuchAlgorithmException | IllegalBlockSizeException | InvalidKeyException | BadPaddingException | NoSuchPaddingException e) {
				Log.e(TAG, "migrateFile(): exception when migrating file "+relativePath, e);
			}
		}
	}

	private static byte[] encrypt(byte[] key, byte[] clear) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
		SecretKeySpec skeySpec = new SecretKeySpec(key, ALG);
		Cipher cipher = Cipher.getInstance(ALG);
		cipher.init(Cipher.ENCRYPT_MODE, skeySpec);

		return cipher.doFinal(clear);
	}

	private static byte[] decrypt(byte[] key, byte[] encrypted) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
		SecretKeySpec skeySpec = new SecretKeySpec(key, ALG);
		Cipher cipher = Cipher.getInstance(ALG);
		cipher.init(Cipher.DECRYPT_MODE, skeySpec);

		return cipher.doFinal(encrypted);
	}

	private static boolean saveFile( Context context, String relativePath, byte[] data ) throws IOException {
		String appPath = context.getFilesDir().getPath();
		String filePath = appPath + "/" + relativePath;
		File file = new File(filePath);

		Boolean hasSubDirs = relativePath.contains("/");
		if(hasSubDirs)
			file.getParentFile().mkdirs();

		FileOutputStream outputStream = new FileOutputStream(file);
		outputStream.write(data);
		outputStream.close();

		return true;
	}

	private static byte[] loadFile( Context context, String relativePath ) throws IOException {
		String appPath = context.getFilesDir().getPath();
		String filePath = appPath + "/" + relativePath;
		File file = new File(filePath);

		byte bytes[] = new byte[(int) file.length()];
		BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
		DataInputStream dis = new DataInputStream(bis);
		dis.readFully(bytes);

		return bytes;
	}

	private static boolean removeFile( Context context, String relativePath ) {
		String appPath = context.getFilesDir().getPath();
		String filePath = appPath + "/" + relativePath;
		File file = new File(filePath);

		if( file.isDirectory() && file.listFiles() != null )
		{
			if( D ) Log.e(TAG, "Can't remove file " + filePath + " because it's an directory and it's not empty!");
			return false;
		}
		else
			return file.delete();
	}

	private static byte[] sha256(String data) throws NoSuchAlgorithmException {
		MessageDigest digest = MessageDigest.getInstance("SHA-256");
		if(digest != null) {
			digest.reset();
			return digest.digest( data.getBytes() ); // getBytes("UTF-8")
		}
		return null;
	}

	private native String fmd5(String data);
}
