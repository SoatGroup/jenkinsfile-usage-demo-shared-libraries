# Soat sample shared library 


This project includes a sample [Global Shared Library](https://jenkins.io/doc/book/pipeline/shared-libraries/) and the project that tests its usage in pipeline scripts using [Jenkins Pipeline Unit](https://github.com/lesfurets/JenkinsPipelineUnit).

## Setup your shared library :

This script explain how to setup a shared library projects :

```bash
# Set the artifact id of the shared library you want to create
export ARTIFACT_ID="soat-sample-shared-libraries"

# Generate a the shared library 
mvn archetype:generate \
        -DgroupId=fr.soat.jenkins \
        -DartifactId=${ARTIFACT_ID} \
        -DarchetypeGroupId=io.jenkins.archetypes \
        -DarchetypeArtifactId=global-shared-library \
        -DinteractiveMode=false

# In the generated directory, move all the content of shared-library/* into the current directory
cd "${ARTIFACT_ID}" && mv shared-library/* . && rm -rf shared-library

# Update the pom.xml to use the proper location
sed -i "s@<directory>..\/shared-library<\/directory>@<directory>..\/<\/directory>@g" unit-tests/pom.xml
```

## Folder structure

* `vars` : Scripts defining variables in pipeline
* `src` : Contains the groovy sources
* `resources` : Resource files
* `unit-tests` : A Apache Maven project for testing the shared library usage with Jenkins Pipeline Unit framework.