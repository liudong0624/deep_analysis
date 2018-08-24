#!/bin/bash
modulename="Embed"

#安装目录
install_path="/opt/WangAn/Embed"

datenow=`date -u +%Y%m%d%H%M`

echo -e "\033[32m "
if [ 'root' != `whoami` ]; then
	echo  "安装过程需要管理员权限，请使用管理员帐户安装。。。"
	echo -e "\033[0m"
	exit 1
fi

echo 'Begin' ${modulename} 'Install...'

if [ -f ${modulename}-2.0-make* ];then
	echo "删除"${modulename}"zip"
	rm -rf ${modulename}-2.0-make*
fi

###################[备份原有目录，创建安装目录]############################
if [ -d ${install_path} ]; then
	echo "安装目录[${install_path}] 已经存在, 备份原有目录"
	mv ${install_path} ${install_path}_${datenow}
fi

#创建安装目录
mkdir -p ${install_path}

###################[ 安装程序]###############################
#将打包的所有文件拷贝到安装目录
cp -rvf * ${install_path}

###################[安装服务]###############################
cd script
echo "安装服务"
service_script_list=`ls`
for file in ${service_script_list}
do
	echo "安装服务脚本[${file}]"
	if [ -f '/etc/init.d/${file}' ]; then
		rm -rf /etc/init.d/${file}
	fi
	chmod 775 ./${file}
	cp ./${file} /etc/init.d/
	#服务开机启动
	/sbin/chkconfig ${file} on
	#前期将就用---将服务注册到守护模块
	if [ ! -d "/root/monitor/" ]; then
		mkdir -p /root/monitor/
	fi
	
	ret=`cat /root/monitor/monitor_list.txt|grep -w ${file} |wc -l`
	if [ 0 -eq ${ret} ]; then
		echo "注册服务[${file}]到守护模块"
		echo ${file} >> /root/monitor/monitor_list.txt
	else
		echo "服务[${file}]已经注册到守护模块"
	fi
done

cd ..

#删除临时目录
cd ..
rm -rf install_temp

echo "${modulename}安装完成"
echo -e "\033[0m"
exit 0
