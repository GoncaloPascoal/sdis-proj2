# SDIS Project

SDIS Project for group T3G22.

Group members:

1. Clara Martins (up201806528@fe.up.pt)
2. Daniel Monteiro (up201806185@fe.up.pt)
3. Gonçalo Pascoal (up201806332@fe.up.pt)
4. Teresa Corado (up201806479@fe.up.pt)

### Compiling
To compile the Peer and TestApp applications, simply run the compile.sh script. The programs can also be compiled using
an IDE such as IntelliJ IDEA.

### Cleanup
To cleanup the file system of a peer, run the cleanup.sh script in the same directory the peer was ran
with the peer's id as an argument. This will attempt to delete the "peer<id>" directory, where <id> is the peer's id.
If the cleanup script is run without any arguments, it will attempt to delete the file systems of all peers.

### Setup
There is no need to do any setup for the peer's filesystem, the application will create the necessary directories and
files when needed. For convenience, files to be backed up and the key stores / trust stores can be moved to the directory 
containing the compiled .class files.

### `rmiregistry`
Before running the Peer or TestApp, make sure that rmiregistry is running by executing
rmiregistry in the directory where the peer will be executed (this is usually the build directory).

### Running Peer
Execute the peer.sh script in the build directory with the desired command-line arguments. The peer_simple.sh script can also
be used, which fills some of the command-line arguments with default values (for example, localhost is used for 
IP addresses, client.keys for the key store path and truststore for the trust store path). These scripts can be run
without arguments to get an explanation of their usage.

### Running TestApp
Simply execute the test.sh script in the build directory with the desired command-line arguments.
