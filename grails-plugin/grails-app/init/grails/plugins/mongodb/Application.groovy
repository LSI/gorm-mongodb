package grails.plugins.mongodb

//import grails.boot.GrailsApp
import grails.plugins.metadata.PluginSource
import groovy.transform.CompileStatic
import org.springframework.context.ConfigurableApplicationContext

@CompileStatic
@PluginSource
class Application {

    static void main(String[] args) {
        ConfigurableApplicationContext result = grails.boot.GrailsApp.run(Application as Class<Application>, args)
    }
}