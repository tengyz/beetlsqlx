call mvn clean:clean
call mvn -Dmaven.test.skip=true install -U -Dgpg.skip
@pause