:: �X�P���g���R�[�h���: API���t�@�����X
set LANG=deviceConnectHtmlDocs

:: �v���t�@�C����`�t�@�C��
set SPEC=..\standard-profile-specs

:: �X�P���g���R�[�h�o�͐�
set OUTPUT_DIR=.\output\html\Device_Connect_RESTful_API_Specification

:: node-gotapi�v���O�C���̕\����
set DISPLAY_NAME=Device_Connect_RESTful_API_Specification

:: �X�P���g���R�[�h�����c�[���̃o�C�i��
set JAR_FILE=..\bin\deviceconnect-codegen.jar

java -Dfile.encoding=UTF-8 -jar %JAR_FILE% --input-spec-dir %SPEC%  --lang %LANG%  --display-name %DISPLAY_NAME%  --output %OUTPUT_DIR%