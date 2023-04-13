#!/usr/bin/bash

declare -A servArray
servArray=(
  ["stock"]="/tmp/stock.jar|/data/jar"
  ["kzz"]="/tmp/kzz.jar|/data/jar"
  ["mock"]="/tmp/mock.jar|/data/jar"
)

servKeyArr="${!servArray[*]}"
backupBaseDir=/data/jar/app-bak
maxBackCount=5

if [ ! -d $backupBaseDir ]; then
  mkdir -p $backupBaseDir
fi

deploy(){
  if [ ! -n "$1" ]; then
    echo "start deploy all app service"
    sh backup_app.sh &> /dev/null
    sh stop_all.sh &>/dev/null
    sh start_all.sh
    echo "deloy all app service done"
  else
    if [[ "${servKeyArr[@]}"  =~ "${1}" ]]; then
      serviceData=$(echo ${servArray["$1"]})
      deployDir=$(echo $serviceData | awk -F '|' '{print $2}')

      echo "start backup service jar"
      backupDir=$backupBaseDir/${1}
      if [ ! -d $backupDir ]; then
        mkdir -p $backupDir
      fi
      curBackCount=$(ls $backupDir | wc -l)
      if [[ $curBackCount -ge $maxBackCount ]]; then
        oldestBackupFile=$(ls $backupDir | grep jar | awk -F- '{print $0}' | sort | head -n 1)
        rm -rf $backupDir/$oldestBackupFile
      fi
      cp $deployDir/*.jar $backupDir/${1}-$(date +"%m%d%H%M").jar

      echo "start kill $1 service"
      uploadFile=$(echo $serviceData | awk -F '|' '{print $1}')
      serviceName=$(echo $uploadFile | awk -F/ '{print $NF}' | cut -d . -f -1)
      pid=$(ps -ef| grep $serviceName | grep -v grep | awk '{print $2}')
      if [ -n "$pid" ]; then
        kill -9 $pid
      fi

      mv $uploadFile $deployDir &>/dev/null
      if [ $? -ne 0 ]; then
        echo "copy deploy jar failed,please check $uploadFile is exists!"
        exit 1
      fi

      echo "start run $1 service"
      cd $deployDir
      {
        sh bootstrap.sh start &>/dev/null
      } &
      sleep 2
      echo "deploy $1 service successful"
    else
      echo "service name input error,only can input[$servKeyArr]"
    fi
  fi
}

rollback(){
 if [ ! -n "$1" ]; then
    echo "no service found for rollback,please define service of [$servKeyArr]"
  else
    if [[ "${servKeyArr[@]}"  =~ "${1}" ]]; then
      serviceData=$(echo ${servArray["$1"]})
      deployDir=$(echo $serviceData | awk -F '|' '{print $2}')

      echo "start kill $1 service"
      uploadFile=$(echo $serviceData | awk -F '|' '{print $1}')
      serviceName=$(echo $uploadFile | awk -F/ '{print $NF}' | cut -d . -f -1)
      pid=$(ps -ef| grep $serviceName | grep -v grep | awk '{print $2}')
      if [ -n "$pid" ]; then
        kill -9 $pid
      fi

      echo "start copy last backup jar"
      backupDir=$backupBaseDir/${1}
      latestBackupFile=$(ls $backupDir | grep jar | awk -F- '{print $0}' | sort -r | head -n 1)
      uploadFile=$(echo $serviceData | awk -F '|' '{print $1}')
      serviceName=$(echo $uploadFile | awk -F/ '{print $NF}' | cut -d . -f -1)
      cp $backupDir/$latestBackupFile $deployDir/${serviceName}.jar

      echo "start run $1 service"
      cd $deployDir
      {
        sh bootstrap.sh start &>/dev/null
      } &
      sleep 2
      echo "rollback $1 service successful"
    else
      echo "service name input error,valid input[$servKeyArr]"
    fi
  fi
}


case "$1" in
"deploy")
  deploy $2
  ;;
"rollback")
  rollback $2
  ;;
*)
  echo "Usage: (you should input at least 1 param,eg:deploy xxx|rollback xxx)"
  ;;
esac