Jenkins Team Foundation Server Trigger Plugin  
==================

このプラグインは TFSのソース管理上で変更があった時にビルドを実行する為に使用します。  

使用するには以下のライブラリをダウンロードして下さい。  
Microsoft Visual Studio Team Foundation Server 2012 Software Development Kit for Java  
http://www.microsoft.com/en-us/download/details.aspx?id=22616

Jenkinsの起動時の引数に下記の変数を追加して下さい。  
<code>-Dcom.microsoft.tfs.jni.native.base-directory=nativeディレクトリ名</code>
