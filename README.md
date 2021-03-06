# M1 Secure Development : Mobile applications

## How I ensure the user is the right one starting the app

In order to make sure that an allowed user is starting the app, I use a masterkey system. As we do not have a database, the masterkey is somehow hardcoded but it is impossible to retreive it by looking at the code because you will only see an ugly version of it. Indeed, I have first encrypted the masterkey with my poly encryption function before hashing it with the SHA1 algorithm. Then, I saved the result and deleted this part so it can't be visible in the source code (in truth I let it in comment for this project as it's a school project to remind me how I did it). So, the only thing of this method remaining hardcoded is the encrypted and hashed version of the masterkey.
Therefore, to know if the user is authorized to enter the app, I encrypt his input with the poly encryption function, then I hash it with the SHA1 algorithm and finally I compare it with the encrypted and hashed version of the masterkey. If the two are equals, then the user can enter the app otherwise an error message shows up.

Of course, the key of my Poly Encryption function is not hardcoded in the source code. I take the input of the user when he tries to connect himself to the app because in that way, the decryption key is the masterkey which is not visible anywhere in the code.

Note that the masterkey is not that complicated as it is a school project but I do know that it would be better to find a more complicated one. However, I checked several website on which we can crack a SHA1 hash with a rainbow table and there is no result for my encrypted masterkey.

For your information, the masterkey is : "VYPAS".

## How I securely save user's data on the phone

To save user's data on the phone I used "this.getExternalFilesDir(null)" which is a Kotlin notation that I can't hide. So, someone who knows a bit of Kotlin or look on the internet would find where the files are located. Therefore, I decided to encrypt the data into the files so it can't be readable by anyone. In order to do so, I used a Poly Encryption function which encrypts each character using a specific key. I could not use the SHA1 algorithm as it can't be reversed and here, I need to encrypt the data so it can't be readable by anyone and decrypt the data so I can display it in the app when the user is authorized to connect himself.

Of course, the key of my Poly Encryption function is not hardcoded in the source code. Actually, as I did for the encryption of the masterkey, I take the input of the user when he tries to connect himself to the app. If his input is correct (matches the masterkey's real value), he is authorized to enter the app which allow the rest of the code to be executed and as the masterkey does not change, I can use his input as a key to encrypt and decrypt the data. Doing so, the key will always be the same because if the input of the user is incorrect (does not matches the masterkey), he will only get an error message and nothing will need to be crypted or decrypted as he will not be able to access the application anyway. 

Therefore, the only way to decrypt the data is to know the masterkey. Doing so, no one can easily decrypt the data as you can only see the encrypted and hashed version of the masterkey in the source code.

For your information, the files from "https://60102f166c21e10017050128.mockapi.io/labbbank/config/X" are stored in "/storage/emulated/0/Android/data/com.example.app_td3/files/config/X" with X being an id and the file from "https://60102f166c21e10017050128.mockapi.io/labbbank/accounts" is stored in "/storage/emulated/0/Android/data/com.example.app_td3/files/accounts".

## How did I hide the API url

To hide the API url, I first put it into a C++ file because this kind of file is not compiled the same way as the rest of the code. In fact, Native C/C++ code is harder to decompile, so hackers will have a harder time gaining access to the data written in this language. I even decompiled my apk file on several website and I never found my C++ file. 

However, I saw on the internet that it is possible to find it using complicated methodes. Therefore, I did not hardcoded the API url in clear into my C++ file but its encrypted version. In order to do so, I used the same function as the one used to encrypt the data stored on the phone and to ensure the security, I also used the same encryption key (input of the user).

Doing so, even if hackers succeed to find the data contained in the C++ file, they will still have to decrypt it.

## Notes

As you can see, everything that is important (API urls, masterkey, data stored on the phone) is encrypted with the same key. The latter is not hardcoded anywhere as it is the input of the user so unless you know the masterkey to access the application, you can't decrypt anything. This method is based on the one used by 1Password for which you have one strong password that will be used to protect and encrypt all your passwords. Of course 1Password uses a more sophisticated method than mine and the password must be stronger than the one I chose but the idea is the same.

## Screenshots of my application 

See the folder "Screenshots" for the pictures and see below what they correspond to :

    * 1 - Connection page : this is the connection page of the application. This is where you can enter the masterkey and the id in order to connect.
    * 2 - Connection Page error masterkey : this is the error you get when you enter a wrong masterkey.
    * 3 - Connection Page error id : this is the error you get when you have the correct masterkey but an error in the id (id does not exist or you do not enter any id or you are offline and have no data saved locally for this id)
    * 4 - Accounts Page : this is the page in which the accounts are displayed. You can also see above that there is the information corresponding to the id you entered in the connection page.
    * 5 - Encryption in file : this is the information corresponding to the id "1" stored locally on the smartphone. As you can see, even if someone succeed to find the file, he won't be able to read it as it is encrypted. Of course, the file containing the information about the accounts is encrypted the same way.
