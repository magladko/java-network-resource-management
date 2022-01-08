rem Start 1 network node, then terminate it
start java NetworkNode -ident 1 -tcpport 9000 A:1 
timeout 1 > NUL
java NetworkClient -gateway localhost:9000 terminate
