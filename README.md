# React native cdn image for remote urls
Secure your keychain in react native app!

### Installing
```
npm install react-native-secure-keychain --save
- or -
yarn add react-native-secure-keychain
```

### Usage
```
import React from 'react';
import RNSecureKeychain from 'react-native-secure-keychain';

export default class Example extends React.Component
{
    constructor()
    {
        //THE MOST IMPORTANT THING! YOU HAVE TO UNLOCK THE KEYCHAIN BEFORE FIRST USAGE!
        RNSecureKeychain.unlock();
    }

    componentDidMount()
    {
        RNSecureKeychain.load('folder/file')
        .then(( content ) =>
		{
            RNSecureKeychain.save('folder/file', {}).catch( err => null );
            RNSecureKeychain.remove('folder/file');
        }).catch( e => {});
    }
}
```
### API

#### unlock
You need to unlock keychain for security reasons. This should be insert before load your UI.
#### load
Load file from keychain. Returns JSON object or string.
#### save
Save file to path. You can save JSON object or string.
#### remove
Remove file at path.