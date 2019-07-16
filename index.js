'use strict';

var RNSecureKeychain = require('react-native').NativeModules.RNSecureKeychain;

module.exports =
{
	unlock: function()
	{
		return RNSecureKeychain.unlock();
	},
	load: function( path )
	{
		return RNSecureKeychain.load( path.toString() ).then( data =>
		{
			try
			{
				data = JSON.parse( data );
			}
			catch(e){}

			return data;
		});
	},
	save: function( path, data )
	{
		return RNSecureKeychain.save( path.toString(), ( typeof data === 'object' ? JSON.stringify( data ) : data ).toString() );
	},
	remove: function( path )
	{
		return RNSecureKeychain.remove(path);
	}
};
