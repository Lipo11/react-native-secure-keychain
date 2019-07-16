#import <UIKit/UIKit.h>
#import <CommonCrypto/CommonCryptor.h>
#import <CommonCrypto/CommonDigest.h>

#import "RNSecureKeychain.h"
#import "iOSStorage.h"

@implementation RNSecureKeychain

RCT_EXPORT_MODULE()

+ (BOOL)requiresMainQueueSetup
{
    return NO;
}

static NSString * s_password = @"";
static NSString * s_iv = @"F68A9A229A516752";

RCT_REMAP_METHOD(unlock,
                 unlockWithResolver:(RCTPromiseResolveBlock)resolve
                 rejecter:(RCTPromiseRejectBlock)reject)
{
    s_password = [[UIDevice currentDevice] identifierForVendor].UUIDString;
    
    resolve([NSNumber numberWithBool:( [s_password isEqualToString:@""] ? NO : YES )]);
}

RCT_REMAP_METHOD(load,
                 path:(NSString *)path
                 readWithResolver:(RCTPromiseResolveBlock)resolve
                 rejecter:(RCTPromiseRejectBlock)reject)
{
    if( [s_password isEqualToString:@""] ){ resolve(@""); return; }
    
    NSString * loadedData = [iOSStorage read:path];
    
    if( [loadedData isEqualToString:@""] )
    {
        reject(@"error", @"not_exists", nil);
    }
    else
    {
        NSData * data = [[NSData alloc] initWithBase64EncodedString:loadedData options:kNilOptions];
        resolve([[NSString alloc] initWithData:[self AES128DecryptedDataWithKey:s_password data:data] encoding:NSUTF8StringEncoding]);
    }
}

RCT_REMAP_METHOD(save,
                 path:(NSString *)path
                 data:(NSString *)data
                 saveWithResolver:(RCTPromiseResolveBlock)resolve
                 rejecter:(RCTPromiseRejectBlock)reject)
{
    if( [s_password isEqualToString:@""] ){ resolve([NSNumber numberWithBool:NO]); return; }
    
    NSData * encrypted = [self AES128EncryptedDataWithKey:s_password data:[data dataUsingEncoding:NSUTF8StringEncoding]];
    resolve([NSNumber numberWithBool:[iOSStorage write:[encrypted base64EncodedStringWithOptions:kNilOptions] to:path]]);
}

RCT_REMAP_METHOD(remove,
                 path:(NSString *)path
                 removeWithResolver:(RCTPromiseResolveBlock)resolve
                 rejecter:(RCTPromiseRejectBlock)reject)
{
    if( [s_password isEqualToString:@""] ){ resolve(@""); return; }
    
    resolve([NSNumber numberWithBool:[iOSStorage remove:path]]);
}

+ (NSString *)stringToHex:(NSString *)str
{
    NSUInteger len = [str length];
    unichar * chars = malloc(len * sizeof(unichar));
    [str getCharacters:chars];
    
    NSMutableString * hexString = [[NSMutableString alloc] init];
    
    for( NSUInteger i = 0; i < len; i++ )
    {
        [hexString appendFormat:@"%02x", chars[i]];
    }
    
    free(chars);
    
    return hexString;
}

+ (NSString*)sha256:(NSString *)data
{
    const char * str = [data UTF8String];
    unsigned char result[CC_SHA256_DIGEST_LENGTH];
    CC_SHA256(str, (unsigned int)strlen(str), result);
    
    NSMutableString * ret = [NSMutableString stringWithCapacity:CC_SHA256_DIGEST_LENGTH*2];
    for( int i = 0; i < CC_SHA256_DIGEST_LENGTH; i++ )
    {
        [ret appendFormat:@"%02x",result[i]];
    }
    
    return ret;
}

- (NSData *)AES128EncryptedDataWithKey:(NSString *)key data:(NSData *)data
{
    return [RNSecureKeychain AES128Operation:kCCEncrypt key:key data:data iv:s_iv];
}

- (NSData *)AES128DecryptedDataWithKey:(NSString *)key data:(NSData *)data
{
    return [RNSecureKeychain AES128Operation:kCCDecrypt key:key data:data iv:s_iv];
}

+ (NSData *)AES128Operation:(CCOperation)operation key:(NSString *)key data:(NSData *)data iv:(NSString *)iv
{
    key = [RNSecureKeychain sha256:key];
    
    char keyPtr[kCCKeySizeAES128 + 1];
    bzero(keyPtr, sizeof(keyPtr));
    [key getCString:keyPtr maxLength:sizeof(keyPtr) encoding:NSUTF8StringEncoding];
    
    char ivPtr[kCCBlockSizeAES128 + 1];
    bzero(ivPtr, sizeof(ivPtr));
    if( iv )
    {
        [iv getCString:ivPtr maxLength:sizeof(ivPtr) encoding:NSUTF8StringEncoding];
    }
    
    NSUInteger dataLength = [data length];
    size_t bufferSize = dataLength + kCCBlockSizeAES128;
    void *buffer = malloc(bufferSize);
    
    size_t numBytesEncrypted = 0;
    CCCryptorStatus cryptStatus = CCCrypt(operation,
                                          kCCAlgorithmAES128,
                                          kCCOptionPKCS7Padding | kCCOptionECBMode,
                                          keyPtr,
                                          kCCBlockSizeAES128,
                                          ivPtr,
                                          [data bytes],
                                          dataLength,
                                          buffer,
                                          bufferSize,
                                          &numBytesEncrypted);
    if( cryptStatus == kCCSuccess )
    {
        return [NSData dataWithBytesNoCopy:buffer length:numBytesEncrypted];
    }
    
    free(buffer);
    return nil;
}

@end
  
