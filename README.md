# SNote

SNote is a secure notebook GUI application written in Java (Swing) and using 256 bit encryption.

It reads and writes encrypted text files. It uses the HC-256 stream encryption described here
[http://www.ecrypt.eu.org/stream/p3ciphers/hc/hc256_p3.pdf]. HC-256 has been extensively studied
and so far no flaws or weaknesses have been found.

The HC-256 encryption is implemented by Bouncy Castle [http://bouncycastle.org] and the Bouncy
Castle libary is required.

The application is a minimal graphical text editor that provides a simple way to read and write
encrypted files. The unencrypted text is never written to disk and only ever exists in memory.

The editor expects encrypted files to end with the extension .enc and open will filter for
files with that ending. Drag and drop of files is also supported.
