#[mnIMAPSync](http://www.marcnuri.com/)

##Description
Java based IMAP Server syncing tool (work in progress).

This tool is inspired in [imapsync](http://imapsync.lamiral.info/). mnIMAPSync is still not reliable
you can use it only for testing purposes, if you need a reliable tool it's advised to use imapsync or any
other alternative. In the last part of this document there is a list of tested IMAP servers, please 
notify me if you successfully use mnIMAPSync with another server or create an issue for failing servers.

mnIMAPSync can be used to migrate or backup an IMAP account to another either in the same server or
 in different servers. The program can be run standalone (command line interface) or accessed directly 
from java.

##Features
- SSL Support
- Multithread
- Api Interface
- Command line execution
- Deletion of no longer existing messages and folders

##Usage
The easiest way is to launch the program from the command-line interface.
Download the latest binary release and execute as follows:

```Batchfile
java -jar mnIMAPSync.jar --host1 imap.gmail.com --port1 993  --user1 user@gmail.com --password1 password --ssl1
 --host2 other.server.com --port2 143 --user2 user2@other.server.com --password2 password2 --delete
```

###Command-line arguments
|Option|Description|
|------|-----------|
|`--host1`*|Host of the source mail server.|
|`--port1`*|IMAP port of the source mail server.|
|`--user1`*|User name for the account on the source mail server.|
|`--password1`*|Password for the account on the source mail server.|
|`--ssl1`|Optional parameter indicating if the program should connect using SSL to the source server.|
|`--host2`*|Host of the target mail server.|
|`--port2`*|IMAP port of the target mail server.|
|`--user2`*|User name for the account on the target mail server.|
|`--password2`*|Password for the account on the target mail server.|
|`--ssl2`|Optional parameter indicating if the program should connect using SSL to the target server.|
|`--delete`|Optional parameter indicating it the program should delete messages and folders in the target server that don't exist in the source.|
\*Required arguments



##Motiviation
When using [imapsync](http://imapsync.lamiral.info/) to sync different servers I'm getting lots of 
duplicate messages in successive runs. This is due to the fact that when dealing with unconventional
e-mails, each server stores certain header information in its own way.

This program is based in the principle of maximizing performance avoiding duplicates. 
If messages that have already been synced are modified (in the non header part [attachments removed...]) 
this messages won't be copied again.

##Syncing process

###Target Indexing

The process starts indexing mail messages and IMAP folders in the target server. This information
will be used later to check if the target server already contains messages we are copying from the source 
server.

The Index is created in a per folder basis. For each IMAP folder in the target server a separate message index 
will be created. Every folder in the target server is crawled.

###Copy process

Once the target index is completed, if and only if this process was successful, the copy process begins.
If there were errors indexing the target the copying process will abort, not aborting could mean duplicating
messages in the target server.

##Tested Servers
- Dovecot
- hMailServer
- Gmail

Please, share your experience with any other server.
