:: スケルトンコード種別: node-gotapiプラグイン
set LANG=gotapiNodePlugin

:: プロファイル定義ファイル
set SPEC=.\sample-profile-specs\swagger-files

:: スケルトンコード出力先
set OUTPUT_DIR=.\output\NodeJS\node-gotapi-plugin-sample

:: テンプレート
set TEMPLATE_DIR="./templates/gotapiNodePlugin"

:: node-gotapiプラグインの表示名
set DISPLAY_NAME=MyPlugin

:: スケルトンコード生成ツールのバイナリ
set JAR_FILE=..\bin\deviceconnect-codegen.jar

java -Dfile.encoding=UTF-8 -jar %JAR_FILE% --input-spec-dir %SPEC%  --template-dir %TEMPLATE_DIR% --lang %LANG%  --display-name %DISPLAY_NAME%  --output %OUTPUT_DIR%
