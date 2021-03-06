/*
 AndroidPluginCodegenConfig.java
 Copyright (c) 2018 NTT DOCOMO,INC.
 Released under the MIT license
 http://opensource.org/licenses/mit-license.php
 */
package org.deviceconnect.codegen.plugin;


import io.swagger.codegen.*;
import io.swagger.models.*;
import io.swagger.models.parameters.FormParameter;
import io.swagger.models.parameters.Parameter;
import io.swagger.models.parameters.QueryParameter;
import io.swagger.models.properties.*;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.lang3.StringUtils;
import org.deviceconnect.codegen.ProfileTemplate;
import org.deviceconnect.codegen.ValidationResult;
import org.deviceconnect.codegen.ValidationResultSet;
import org.deviceconnect.codegen.util.VersionName;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class AndroidPluginCodegenConfig extends AbstractPluginCodegenConfig {

    public enum ConnectionType {
        BROADCAST,
        BINDER
    }
    private static final String DEVICE_PLUGIN_SDK_VERSION_NAME = "2.8.4";
    private final String pluginModuleFolder = "plugin";
    private final String projectFolder = pluginModuleFolder + "/src/main";
    private final String sourceFolder = projectFolder + "/java";
    private final String resFolder = projectFolder + "/res";
    private String invokerPackage;
    private ConnectionType connectionType = ConnectionType.BINDER;

    private final String apiDocPath = "docs/";
    private final String modelDocPath = "docs/";

    {
        putOption("targetSdkVersion", "", "29");
        putOption("minSdkVersion", "", "19");
        putOption("compileSdkVersion", "", "29");
        putOption("deviceConnectPluginSdkVersion", "", DEVICE_PLUGIN_SDK_VERSION_NAME);
        putOption("deviceConnectSdkForAndroidVersion", "", "2.3.2");
        putOption("githubAccountName", "", "YOUR_NAME");
        putOption("githubAccountKey", "", "YOUR_KEY");
    }

    private void putOption(final String name, final String description, final String defaultValue) {
        CliOption option = CliOption.newString(name, description);
        option.setDefault(defaultValue);
        this.cliOptions.add(option);
        this.additionalProperties.put(name, defaultValue);
    }

    @Override
    public ValidationResultSet validateOptions(final CommandLine cmd, final ClientOpts clientOpts) {
        ValidationResultSet resultSet = new ValidationResultSet();
        resultSet.addResult(readPackageName(cmd, clientOpts));
        resultSet.addResult(readConnectionType(cmd));
        resultSet.addResult(readSDK(cmd, clientOpts));
        resultSet.addResult(readSigningConfigs(cmd, clientOpts));
        return resultSet;
    }

    private ValidationResult readPackageName(final CommandLine cmd, final ClientOpts clientOpts) {
        // パッケージ名の指定
        String packageName = cmd.getOptionValue("p", getDefaultPackageName());
        clientOpts.getProperties().put("packageName", packageName);
        return ValidationResult.valid("p");
    }

    private ValidationResult readConnectionType(final CommandLine cmd) {
        // 連携タイプの指定
        String value = cmd.getOptionValue("b", "binder");
        if ("broadcast".equals(value)) {
            connectionType = ConnectionType.BROADCAST;
        } else if ("binder".equals(value)) {
            connectionType = ConnectionType.BINDER;
        } else {
            return ValidationResult.invalid("b", "Undefined connection type: " + value);
        }
        return ValidationResult.valid("b");
    }

    private ValidationResult readSDK(final CommandLine cmd, final ClientOpts clientOpts) {
        final String opt = "k";

        String value = cmd.getOptionValue(opt);
        if (value == null) {
            return ValidationResult.valid(opt);
        }
        String absolutePath;
        try {
            absolutePath = readPath(value);
        } catch (IOException e) {
            return ValidationResult.invalid(opt, value + " is not resolved.");
        }
        if (absolutePath == null) {
            return ValidationResult.invalid(opt, value + " is not found.");
        }
        clientOpts.getProperties().put("sdkLocation", absolutePath);
        return ValidationResult.valid(opt);
    }

    private ValidationResult readSigningConfigs(final CommandLine cmd, final ClientOpts clientOpts) {
        final String opt = "g";

        String value = cmd.getOptionValue(opt);
        if (value == null) {
            return ValidationResult.valid(opt);
        }
        String absolutePath;
        try {
            absolutePath = readPath(value);
        } catch (IOException e) {
            return ValidationResult.invalid(opt, value + " is not resolved.");
        }
        if (absolutePath == null) {
            return ValidationResult.invalid(opt, value + " is not found.");
        }
        clientOpts.getProperties().put("signingConfigsLocation", absolutePath);
        return ValidationResult.valid(opt);
    }

    private String readPath(final String value) throws IOException {
        File file = new File(value);
        if (!file.exists()) {
            return null;
        }
        String path = file.getCanonicalPath();
        if (File.separator.equals("\\")) {
            path = path.replace("\\", "/");
        }
        return path;
    }

    //----- AbstractPluginCodegenConfig ----//

    @Override
    public String getDefaultPackageName() {
        return "com.mydomain.myplugin";
    }

    @Override
    protected String profileFileFolder() {
        String separator = File.separator;
        return outputFolder + separator + sourceFolder + separator + getProfilePackage().replace('.', File.separatorChar);
    }

    @Override
    protected String getProfileSpecFolder() {
        String separator = File.separator;
        return outputFolder + separator + getSpecFolder();
    }

    @Override
    protected List<ProfileTemplate> prepareProfileTemplates(final String profileName, final Map<String, Object> properties) {
        final List<ProfileTemplate> profileTemplates = new ArrayList<>();
        String baseClassNamePrefix = getStandardClassName(profileName);
        final String baseClassName;
        final String profileClassName;
        final boolean isStandardProfile = baseClassNamePrefix != null;
        if (isStandardProfile) {
            baseClassName = baseClassNamePrefix + "Profile";
            profileClassName = getClassPrefix() + baseClassNamePrefix + "Profile";
        } else {
            baseClassName = "DConnectProfile";
            profileClassName = getClassPrefix() + toUpperCapital(profileName) + "Profile";
        }
        properties.put("baseProfileClass", baseClassName);
        properties.put("profileClass", profileClassName);
        properties.put("profileNameDefinition", profileName);
        properties.put("profilePackage", getProfilePackage());
        properties.put("isStandardProfile", isStandardProfile);

        ((List<Object>) additionalProperties.get("supportedProfileClasses")).add(new Object() { String name = profileClassName;});

        ProfileTemplate template = new ProfileTemplate();
        template.templateFile = "java" + File.separator + "profile.mustache";
        template.outputFile = profileClassName + ".java";
        profileTemplates.add(template);
        return profileTemplates;
    }

    private String getProfilePackage() {
        return invokerPackage + ".profiles";
    }

    //----- CodegenConfig ----//

    @Override
    public CodegenType getTag() { return CodegenType.OTHER; }

    @Override
    public String getName() { return "deviceConnectAndroidPlugin"; }

    @Override
    public String getHelp() { return "Generates a stub of Device Connect Plug-in for Android."; }

    @Override
    public String apiFileFolder() {
        return outputFolder + "/" + sourceFolder + "/" + apiPackage().replace('.', File.separatorChar);
    }

    @Override
    public String apiDocFileFolder() {
        return (outputFolder + "/" + apiDocPath).replace( '/', File.separatorChar );
    }

    @Override
    public String modelFileFolder() {
        return outputFolder + "/" + sourceFolder + "/" + modelPackage().replace('.', File.separatorChar);
    }

    @Override
    public String modelDocFileFolder() {
        return ( outputFolder + "/" + modelDocPath ).replace( '/', File.separatorChar );
    }

    @Override
    public String toParamName(String name) {
        // should be the same as variable name
        return toVarName(name);
    }

    @Override
    public String escapeReservedWord(String name) {
        return "_" + name;
    }

    @Override
    public String getTypeDeclaration(Property p) {
        if (p instanceof ArrayProperty) {
            ArrayProperty ap = (ArrayProperty) p;
            Property inner = ap.getItems();
            return getSwaggerType(p) + "<" + getTypeDeclaration(inner) + ">";
        } else if (p instanceof MapProperty) {
            MapProperty mp = (MapProperty) p;
            Property inner = mp.getAdditionalProperties();

            return getSwaggerType(p) + "<String, " + getTypeDeclaration(inner) + ">";
        }
        return super.getTypeDeclaration(p);
    }

    @Override
    public void processOpts() {
        super.processOpts();
        invokerPackage = (String) additionalProperties.get("packageName");
        embeddedTemplateDir = getName();
        additionalProperties.put("profilePackage", getProfilePackage());
        additionalProperties.put("devicePluginXml", getDevicePluginXmlName());
        additionalProperties.put("specPath", getSpecPath());

        final String classPrefix = getClassPrefix();
        final String messageServiceClass = classPrefix + "MessageService";
        final String messageServiceProviderClass = classPrefix + "MessageServiceProvider";
        additionalProperties.put(CodegenConstants.INVOKER_PACKAGE, invokerPackage);
        additionalProperties.put("serviceId", classPrefix.toLowerCase() + "_service_id");
        additionalProperties.put("messageServiceClass", messageServiceClass);
        additionalProperties.put("messageServiceProviderClass", messageServiceProviderClass);

        // README
        supportingFiles.add(new SupportingFile("README.md.mustache", "", "README.md"));

        // Gradle Wrapper
        supportingFiles.add(new SupportingFile("gradleFiles/wrapper/gradlew", "", "gradlew"));
        supportingFiles.add(new SupportingFile("gradleFiles/wrapper/gradlew.bat", "", "gradlew.bat"));
        supportingFiles.add(new SupportingFile("gradleFiles/wrapper/gradle-wrapper.jar", "gradle/wrapper", "gradle-wrapper.jar"));
        supportingFiles.add(new SupportingFile("gradleFiles/wrapper/gradle-wrapper.properties", "gradle/wrapper", "gradle-wrapper.properties"));

        // ビルド設定ファイル
        supportingFiles.add(new SupportingFile("gradleFiles/settings.gradle.mustache", "", "settings.gradle"));
        String manifest = getManifestTemplateFile();
        supportingFiles.add(new SupportingFile("manifest/" + manifest, getProjectDir(), "AndroidManifest.xml"));
        supportingFiles.add(new SupportingFile(getGradleTemplateDir()+ "/root.build.gradle.mustache", "", "build.gradle"));
        if (getGradleTemplateFile().indexOf(DEVICE_PLUGIN_SDK_VERSION_NAME) != -1) {
            supportingFiles.add(new SupportingFile("github.properties.mustache", "", "github.properties"));
            supportingFiles.add(new SupportingFile(getGradleTemplateDir() + "/plugin.2.8.4.build.gradle.mustache", getPluginModuleDir(), "build.gradle"));
        } else{
            supportingFiles.add(new SupportingFile(getGradleTemplateDir() + "/plugin.build.gradle.mustache", getPluginModuleDir(), "build.gradle"));
        }
        supportingFiles.add(new SupportingFile("gradle.properties.mustache", "", "gradle.properties"));
        supportingFiles.add(new SupportingFile("resource/xml/deviceplugin.xml.mustache", getPluginResourceDir() + "/xml", getDevicePluginXmlName() + ".xml"));
        supportingFiles.add(new SupportingFile("resource/values/strings.xml.mustache", getPluginResourceDir() + "/values", "strings.xml"));

        // リソース
        supportingFiles.add(new SupportingFile("resource/layout/activity_setting.xml", getPluginResourceDir() + "/layout/", "activity_setting.xml"));
        supportingFiles.add(new SupportingFile("resource/drawable-mdpi/ic_launcher.png", getPluginResourceDir() + "/drawable-mdpi/", "ic_launcher.png"));
        supportingFiles.add(new SupportingFile("resource/drawable-hdpi/ic_launcher.png", getPluginResourceDir() + "/drawable-hdpi/", "ic_launcher.png"));
        supportingFiles.add(new SupportingFile("resource/drawable-xhdpi/ic_launcher.png", getPluginResourceDir() + "/drawable-xhdpi/", "ic_launcher.png"));
        supportingFiles.add(new SupportingFile("resource/drawable-xxhdpi/ic_launcher.png", getPluginResourceDir() + "/drawable-xxhdpi/", "ic_launcher.png"));

        // 実装ファイル (全プラグイン共通)
        final String packageFolder = getPluginPackageRootDir();
        supportingFiles.add(new SupportingFile("java/MessageServiceProvider.java.mustache", packageFolder, messageServiceProviderClass + ".java"));
        supportingFiles.add(new SupportingFile("java/MessageService.java.mustache", packageFolder, messageServiceClass + ".java"));
        supportingFiles.add(new SupportingFile("java/SystemProfile.java.mustache", packageFolder + File.separator + "profiles", classPrefix + "SystemProfile.java"));
        supportingFiles.add(new SupportingFile("java/SettingActivity.java.mustache", packageFolder, classPrefix + "SettingActivity.java"));
        if (connectionType == ConnectionType.BROADCAST) {
            additionalProperties.put("launchServiceClass", classPrefix + "LaunchService");
            supportingFiles.add(new SupportingFile("java/LaunchService.java.mustache", packageFolder, classPrefix + "LaunchService.java"));
        }
    }

    protected String getManifestTemplateFile() {
        String manifest;
        switch (connectionType) {
            case BROADCAST:
                manifest = "manifest.broadcast.mustache";
                break;
            case BINDER:
                manifest = "manifest.binder.mustache";
                break;
            default:
                throw new RuntimeException("Unknown connection type");
        }
        return manifest;
    }

    protected String getGradleTemplateFile() {
        String gradleVersion;
        final VersionName ver2_8_4 = VersionName.parse(DEVICE_PLUGIN_SDK_VERSION_NAME);
        String versionParam = (String) additionalProperties.get("deviceConnectPluginSdkVersion");
        VersionName version = VersionName.parse(versionParam);
        if (version == null) {
            version = ver2_8_4;
        }
        if (version.isEqualOrMoreThan(ver2_8_4)) {
            gradleVersion = DEVICE_PLUGIN_SDK_VERSION_NAME + ".";
        } else {
            gradleVersion = "";
        }
        return gradleVersion;
    }

    protected String getPluginSourceDir() {
        return sourceFolder;
    }

    protected String getProjectDir() {
        return projectFolder;
    }

    protected String getPluginModuleDir() {
        return pluginModuleFolder;
    }

    protected String getPluginResourceDir() {
        return resFolder;
    }

    protected String getPluginPackageRootDir() {
        return (getPluginSourceDir() + File.separator + invokerPackage).replace(".", File.separator);
    }

    private String getGradlePluginVersion() {
        return (String) additionalProperties.get("gradlePluginVersion");
    }

    private String getGradleTemplateDir() {
        final VersionName ver3 = VersionName.parse("3.0.0");

        String versionParam = getGradlePluginVersion();
        VersionName version = VersionName.parse(versionParam);
        if (version == null) {
            version = ver3;
        }
        String dirName;
        if (version.isEqualOrMoreThan(ver3)) {
            dirName = "3_x_x";
        } else {
            dirName = "2_x_x";
        }
        return "gradleFiles/" + dirName;
    }

    @Override
    public Map<String, Object> postProcessOperations(Map<String, Object> objs) {
        return super.postProcessOperations(objs);
    }

    @Override
    public String toModelFilename(String name) {
        // should be the same as the model name
        return toModelName(name);
    }

    @Override
    public String toApiDocFilename( String name ) {
        return toApiName( name );
    }

    @Override
    public String toModelDocFilename( String name ) {
        return toModelName( name );
    }

    //----- DefaultCodegen ----//

    @Override
    public String getSwaggerType(Property p) {
        String swaggerType = super.getSwaggerType(p);
        String type = null;
        if (typeMapping.containsKey(swaggerType)) {
            type = typeMapping.get(swaggerType);
            if (languageSpecificPrimitives.contains(type) || type.indexOf(".") >= 0 ||
                    type.equals("Map") || type.equals("List") ||
                    type.equals("File") || type.equals("Date")) {
                return type;
            }
        } else {
            type = swaggerType;
        }
        return toModelName(type);
    }

    @Override
    public String toVarName(String name) {
        // sanitize name
        name = sanitizeName(name); // FIXME: a parameter should not be assigned. Also declare the methods parameters as 'final'.

        // replace - with _ e.g. created-at => created_at
        name = name.replaceAll("-", "_"); // FIXME: a parameter should not be assigned. Also declare the methods parameters as 'final'.

        // if it's all upper case, do nothing
        if (name.matches("^[A-Z_]*$")) {
            return name;
        }

        // camelize (lower first character) the variable name
        // pet_id => petId
        name = camelize(name, true);

        // for reserved word or word starting with number, append _
        if (isReservedWord(name) || name.matches("^\\d.*")) {
            name = escapeReservedWord(name);
        }

        return name;
    }

    @Override
    public void setParameterExampleValue(CodegenParameter p) {
        String example;

        if (p.defaultValue == null) {
            example = p.example;
        } else {
            example = p.defaultValue;
        }

        String type = p.baseType;
        if (type == null) {
            type = p.dataType;
        }

        if ("String".equals(type)) {
            if (example == null) {
                example = p.paramName + "_example";
            }
            example = "\"" + escapeText(example) + "\"";
        } else if ("Integer".equals(type) || "Short".equals(type)) {
            if (example == null) {
                example = "56";
            }
        } else if ("Long".equals(type)) {
            if (example == null) {
                example = "56";
            }
            example = example + "L";
        } else if ("Float".equals(type)) {
            if (example == null) {
                example = "3.4";
            }
            example = example + "F";
        } else if ("Double".equals(type)) {
            example = "3.4";
            example = example + "D";
        } else if ("Boolean".equals(type)) {
            if (example == null) {
                example = "true";
            }
        } else if ("File".equals(type)) {
            if (example == null) {
                example = "/path/to/file";
            }
            example = "new File(\"" + escapeText(example) + "\")";
        } else if ("Date".equals(type)) {
            example = "new Date()";
        } else if (!languageSpecificPrimitives.contains(type)) {
            // type is a model class, e.g. User
            example = "new " + type + "()";
        }

        if (example == null) {
            example = "null";
        } else if (Boolean.TRUE.equals(p.isListContainer)) {
            example = "Arrays.asList(" + example + ")";
        } else if (Boolean.TRUE.equals(p.isMapContainer)) {
            example = "new HashMap()";
        }

        p.example = example;
    }

    @Override
    public String toOperationId(String operationId) {
        // throw exception if method name is empty
        if (StringUtils.isEmpty(operationId)) {
            throw new RuntimeException("Empty method name (operationId) not allowed");
        }

        operationId = camelize(sanitizeName(operationId), true);

        // method name cannot use reserved keyword, e.g. return
        if (isReservedWord(operationId)) {
            String newOperationId = camelize("call_" + operationId, true);
            LOGGER.warn(operationId + " (reserved word) cannot be used as method name. Renamed to " + newOperationId);
            return newOperationId;
        }

        return operationId;
    }

    @Override
    protected String getDeclaration(final Parameter p) {
        String type;
        String format;
        if (p instanceof QueryParameter) {
            type = ((QueryParameter) p).getType();
            format = ((QueryParameter) p).getFormat();
        } else if (p instanceof FormParameter) {
            type = ((FormParameter) p).getType();
            format = ((FormParameter) p).getFormat();
        } else {
            return null;
        }

        String varName = p.getName();
        String typeName;
        boolean hasParser;
        if ("number".equals(type)) {
            if ("double".equals(format)) {
                typeName = "Double";
            } else {
                typeName = "Float";
            }
            hasParser = true;
        } else if ("integer".equals(type)) {
            if ("int64".equals(format)) {
                typeName = "Long";
            } else {
                typeName = "Integer";
            }
            hasParser = true;
        } else if ("boolean".equals(type)) {
            typeName = "Boolean";
            hasParser = true;
        } else if ("string".equals(type) || "array".equals(type)) {
            typeName = "String";
            hasParser = false;
        } else if ("file".equals(type)) {
            typeName = "byte[]";
            hasParser = false;
        } else {
            typeName = "Object";
            hasParser = false;
        }
        String leftOperand = typeName + " " + varName;
        String rightOperand;
        if (hasParser) {
            rightOperand = "parse" + typeName + "(request, \"" + varName + "\")";
        } else {
            rightOperand = "(" + typeName + ") request.getExtras().get(\"" + varName + "\")";
        }
        return leftOperand + " = " + rightOperand + ";";
    }

    @Override
    protected List<String> getResponseCreation(final Swagger swagger, final Response response) {
        List<String> lines = new ArrayList<>();
        Property schema = response.getSchema();

        ObjectProperty root;
        if (schema instanceof ObjectProperty) {
            root = (ObjectProperty) schema;
        } else if (schema instanceof RefProperty) {
            RefProperty ref = (RefProperty) schema;
            if (isIgnoredDefinition(ref.getName())) {
                return lines;
            }
            Model model = findDefinition(swagger, ref.getSimpleRef());
            Map<String, Property> properties;
            if (model instanceof ComposedModel) {
                properties = getProperties(swagger, (ComposedModel) model);
            } else if (model instanceof ModelImpl) {
                properties = model.getProperties();
            } else {
                lines.add("// WARNING: レスポンスの定義が不正です.");
                return lines;
            }
            if (properties == null) {
                lines.add("// WARNING: レスポンスの定義が見つかりませんでした.");
                return lines;
            }
            root =  new ObjectProperty();
            root.setProperties(properties);
        } else {
            lines.add("// WARNING: レスポンスの定義が不正です.");
            return lines;
        }

        Map<String, Property> props = root.getProperties();
        if (props != null && props.size() > 0) {
            writeExampleResponse(swagger, root, "root", lines);
        }
        return lines;
    }

    @Override
    protected List<String> getEventCreation(final Swagger swagger, final Response event) {
        List<String> lines = new ArrayList<>();
        Property schema = event.getSchema();

        ObjectProperty root;
        if (schema instanceof ObjectProperty) {
            root = (ObjectProperty) schema;
        } else if (schema instanceof RefProperty) {
            RefProperty ref = (RefProperty) schema;
            if (isIgnoredDefinition(ref.getName())) {
                return lines;
            }
            Model model = findDefinition(swagger, ref.getSimpleRef());
            Map<String, Property> properties;
            if (model instanceof ComposedModel) {
                properties = getProperties(swagger, (ComposedModel) model);
            } else if (model instanceof ModelImpl) {
                properties = model.getProperties();
            } else {
                lines.add("// WARNING: イベントの定義が不正です.");
                return lines;
            }
            if (properties == null) {
                lines.add("// WARNING: イベントの定義が見つかりませんでした.");
                return lines;
            }
            root =  new ObjectProperty();
            root.setProperties(properties);
        } else {
            lines.add("// WARNING: イベントの定義が不正です.");
            return lines;
        }

        Map<String, Property> props = root.getProperties();
        if (props != null && props.size() > 0) {
            writeExampleEvent(swagger, root, "root", lines);
        }
        return lines;
    }

    private void writeExampleResponse(final Swagger swagger,
                                      final ObjectProperty root,
                                      final String rootName,
                                      final List<String> lines) {
        lines.add("Bundle " + rootName + " = response.getExtras();");
        writeExampleMessage(swagger, root, rootName, "", lines);
        lines.add("response.putExtras(" + rootName + ");");
    }

    private void writeExampleEvent(final Swagger swagger,
                                   final ObjectProperty root,
                                   final String rootName,
                                   final List<String> lines) {
        lines.add("Bundle " + rootName + " = message.getExtras();");
        writeExampleMessage(swagger, root, rootName, "", lines);
        lines.add("message.putExtras(" + rootName + ");");
    }

    private void writeExampleMessage(final Swagger swagger,
                                     final ObjectProperty root,
                                     final String rootName,
                                     final String objectNamePrefix,
                                     final List<String> lines) {
        writeExampleMessage(swagger, root.getProperties(), rootName, objectNamePrefix, lines);
    }

    private void writeExampleMessage(final Swagger swagger,
                                     final Map<String, Property> props,
                                     final String rootName,
                                     final String objectNamePrefix,
                                     final List<String> lines) {
        if (props == null) {
            return;
        }
        for (Map.Entry<String, Property> propEntry : props.entrySet()) {
            String propName = propEntry.getKey();
            Property prop = propEntry.getValue();

            String type = prop.getType();
            String format = prop.getFormat();
            if ("array".equals(type)) {
                System.out.println("writeExampleMessage: array: " + prop.getClass());
                if (!(prop instanceof ArrayProperty)) {
                    continue;
                }
                ArrayProperty arrayProp = (ArrayProperty) prop;
                Property itemsProp = arrayProp.getItems();
                System.out.println("writeExampleMessage: itemsProp: " + itemsProp.getClass());
                if (itemsProp instanceof RefProperty) {
                    RefProperty refItemsProp = (RefProperty) itemsProp;
                    System.out.println("writeExampleMessage: refItemsProp: SimpleRef = " + refItemsProp.getSimpleRef()
                            + ", $ref = " + refItemsProp.get$ref()
                            + ", RefFormat = " + refItemsProp.getRefFormat());
                    Model itemModel = findDefinition(swagger, refItemsProp.getSimpleRef());
                    writeArrayExample(swagger, rootName, propName, itemModel.getProperties(), objectNamePrefix, lines);
                } else {
                    String arrayClassName = getArrayClassName(itemsProp);
                    if (arrayClassName == null) {
                        continue;
                    }
                    lines.add(arrayClassName + "[] " + propName + " = new " + arrayClassName + "[1];");
                    if ("object".equals(itemsProp.getType())) {
                        writeArrayExample(swagger, rootName, propName, ((ObjectProperty) itemsProp).getProperties(), objectNamePrefix, lines);
                    } else {
                        lines.add(propName + "[0] = " + getExampleValue(itemsProp) + ";");
                        String setterName = getSetterName(itemsProp.getType(), itemsProp.getFormat());
                        if (setterName == null) {
                            continue;
                        }
                        lines.add(rootName + "." + setterName +  "Array(\"" + propName + "\", " + propName + ");");
                    }
                }
            } else if ("object".equals(type)) {
                ObjectProperty objectProp;
                if (!(prop instanceof ObjectProperty)) {
                    continue;
                }
                objectProp = (ObjectProperty) prop;
                String objectPropName = getObjectName(objectNamePrefix, propName);
                lines.add("Bundle " + objectPropName + " = new Bundle();");
                writeExampleMessage(swagger, objectProp, objectPropName, objectPropName, lines);
                lines.add(rootName  + ".putBundle(\"" + propName + "\", " + objectPropName + ");");
            } else {
                String setterName = getSetterName(type, format);
                if (setterName == null) {
                    continue;
                }
                lines.add(rootName + "." + setterName +  "(\""+ propName + "\", " + getExampleValue(prop) + ");");
            }
        }
    }

    private void writeArrayExample(final Swagger swagger,
                                   final String rootName,
                                   final String propName,
                                   final Map<String, Property> props,
                                   final String objectNamePrefix,
                                   final List<String> lines) {
        String index = "0";
        String objectPropName = getObjectName(objectNamePrefix, propName);
        String arrayPropName = objectPropName  + "[" + index + "]";
        lines.add(arrayPropName + " = new Bundle();");
        writeExampleMessage(swagger, props, arrayPropName, objectPropName, lines);
        lines.add(rootName + ".putParcelableArray(\"" + propName + "\", " + objectPropName + ");");
    }

    private String getObjectName(final String rootName, final String name) {
        return concatAsCamelCase(rootName, name);
    }

    private String concatAsCamelCase(final String a, final String b) {
        if (isEmpty(a) && isEmpty(b)) {
            return "";
        }
        if (isEmpty(a)) {
            return b;
        }
        if (isEmpty(b)) {
            return a;
        }
        return a + capitalize(b);
    }

    private boolean isEmpty(final String str) {
        return "".equals(str);
    }

    private String capitalize(final String str) {
        if (str == null) {
            return null;
        }
        if (isEmpty(str)) {
            return "";
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    private String getSetterName(final String type, final String format) {
        if ("boolean".equals(type)) {
            return "putBoolean";
        } else if ("string".equals(type)) {
            return "putString";
        } else if ("integer".equals(type)) {
            if ("int64".equals(format)) {
                return "putLong";
            } else {
                return "putInt";
            }
        } else if ("number".equals(type)) {
            if ("double".equals(format)) {
                return "putDouble";
            } else {
                return "putFloat";
            }
        } else {
            // 現状のプラグインでは下記のタイプは非対応.
            //  - file
            return null;
        }
    }

    private String getClassName(final Property prop) {
        final String type = prop.getType();
        final String format = prop.getFormat();
        if ("object".equals(type)) {
            return "Bundle";
        } else if ("boolean".equals(type)) {
            return "Boolean";
        } else if ("string".equals(type)) {
            return "String";
        } else if ("integer".equals(type)) {
            if ("int64".equals(format)) {
                return "Long";
            } else {
                return "Integer";
            }
        } else if ("number".equals(type)) {
            if ("double".equals(format)) {
                return "Double";
            } else {
                return "Float";
            }
        } else {
            // 現状のプラグインでは下記のタイプは非対応.
            //  - file
            return null;
        }
    }

    private String getArrayClassName(final Property prop) {
        final String type = prop.getType();
        final String format = prop.getFormat();
        if ("object".equals(type)) {
            return "Bundle";
        } else if ("boolean".equals(type)) {
            return "boolean";
        } else if ("string".equals(type)) {
            return "String";
        } else if ("integer".equals(type)) {
            if ("int64".equals(format)) {
                return "long";
            } else {
                return "int";
            }
        } else if ("number".equals(type)) {
            if ("double".equals(format)) {
                return "double";
            } else {
                return "float";
            }
        } else {
            // 現状のプラグインでは下記のタイプは非対応.
            //  - file
            return null;
        }
    }

    // TODO: 他のプラットフォームと共通化する
    private String getExampleValue(final Property prop) {
        final String type = prop.getType();
        final String format = prop.getFormat();
        if ("boolean".equals(type)) {
            return "false";
        } else if ("string".equals(type)) {
            if ("date-time".equals(format)) {
                return "\"2000-01-01T01:01:0+09:00\"";
            } else if ("date".equals(format)) {
                return "\"2000-01-01\"";
            } else if ("byte".equals(format)) {
                return "\"dGVzdA==\"";
            } else if ("binary".equals(format)) {
                return "\"\""; // TODO: バイナリ形式の文字列表現の仕様を確認
            } else {
                StringProperty strProp = (StringProperty) prop;
                List<String> enumList = strProp.getEnum();
                if (enumList != null && enumList.size() > 0) {
                    return "\"" + enumList.get(0) + "\"";
                } else {
                    return "\"test\"";
                }
            }
        } else if ("integer".equals(type)) {
            if ("int64".equals(format)) {
                return "0L";
            } else {
                return "0";
            }
        } else if ("number".equals(type)) {
            if ("double".equals(format)) {
                return "0.0d";
            } else {
                return "0.0f";
            }
        } else {
            // 現状のプラグインでは下記のタイプは非対応.
            //  - file
            return null;
        }
    }

    private String getDevicePluginXmlName() {
        return escapePackageName(invokerPackage);
    }

    private String getSpecPath() {
        final String SP = File.separator;
        return escapePackageName(invokerPackage) + SP + "api";
    }

    private String getSpecFolder() {
        final String SP = File.separator;
        return projectFolder + SP + "assets" + SP + getSpecPath();

    }
    private String escapePackageName(final String packageName) {
        if (packageName == null) {
            throw new NullPointerException("packageName is null.");
        }
        return packageName.replace('.', '_');
    }
}