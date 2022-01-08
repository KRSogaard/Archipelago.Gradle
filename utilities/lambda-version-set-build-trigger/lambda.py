import json
import urllib.parse
import boto3
import zipfile

print('Loading function')

s3 = boto3.client('s3', region_name='us-west-2')

bucket = "codepipeline-trigger.archipelago.build"

def lambda_handler(event, context):
    versionSet = event['versionSet'];
    revision = event['revision'];
    
    print("VS: " + versionSet + ", Rev:" + revision)

    fileName = versionSet + ".zip";
    tmpPath = "/tmp/" + fileName;

    zf = zipfile.ZipFile(tmpPath, mode="w", compression=zipfile.ZIP_DEFLATED)
    zf.writestr("version-set-build.sh", "export ARCHIPELAGO_VERSION_SET=" + versionSet + "\n" + 
                "export ARCHIPELAGO_VERSION_REVISION=" + revision)
    zf.close()
    
    try:
        s3.upload_file(tmpPath, bucket, fileName)
    except Exception as e:
        print(e)
        raise e
