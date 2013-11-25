#[mnIMAPSync](http://www.marcnuri.com/)

##Description
Java based IMAP Server syncing tool (work in progress).

This tool is inspired in [imapsync](http://imapsync.lamiral.info/). mnIMAPSync is still not reliable
you can use it only for testing purposes, if you need a reliable tool it's advised to use imapsync or any
other alternative.

mnIMAPSync can be used to migrate or backup an IMAP account to another either in the same server or
 in different servers. The program can be run standalone (command line interface) or accessed directly 
from java.

##Features
- SSL Support
- Multithread

##Motiviation
When using [imapsync](http://imapsync.lamiral.info/) to sync different servers I'm getting lots of 
duplicate messages in successive runs. This is due to the fact that when dealing with unconventional
e-mails, each server stores certain header information in its own way.

This program is based in the principle of maximizing performance avoiding duplicates. 
If messages that have already been synced are modified (in the non header part [attachments removed...]) 
this messages won't be copied again.

##Tested Servers
- Dovecot
- hMailServer
