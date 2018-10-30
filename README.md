运行源码步骤：
* 运行android studio
* 在android studio中选择从github中导入本项目
* 数据线连接android手机，运行程序即可（手机可能需要切换到开发者模式，请自行google），此时程序会连接到牛牛背单词网站的服务器
* 注意，由于android端需要使用服务端的VO类，所以需要首先下载服务端源码，并mvn clean install，把VO类安装到本地maven仓库, 否则android端的源码通不过编译
