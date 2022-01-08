rem Start 2 network nodes, then terminate them
start java NetworkNode -ident 1 -tcpport 9000 A:1 
timeout 1 > NUL
start java NetworkNode -ident 1 -tcpport 9001 -gateway localhost:9000 A:1 
timeout 1 > NUL
java NetworkClient -gateway localhost:9000 terminate
