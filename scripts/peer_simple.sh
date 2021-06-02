if [ "$#" -lt 2 ]; then
  echo "Usage: $0 <id> <port> [contact_port]"
  exit 1
fi
if [ "$#" -eq 3 ]; then
  java protocol.Peer 1.0 $1 rmi$1 client.keys truststore 123456 localhost $2 localhost $3
  exit 0
fi
java protocol.Peer 1.0 $1 rmi$1 client.keys truststore 123456 localhost $2