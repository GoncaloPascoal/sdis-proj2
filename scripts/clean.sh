if [ "$#" -eq 0 ]; then
  rm -rf peer*/
  exit 0
fi
rm -rf peer$1/