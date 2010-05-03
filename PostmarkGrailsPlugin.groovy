import org.apache.commons.logging.LogFactory

class PostmarkGrailsPlugin {
    // the plugin version
    def version = "0.2"
    // the version or versions of Grails the plugin is designed for
    def grailsVersion = "1.3.0.RC1 > *"
    // the other plugins this plugin depends on
    def dependsOn = [rest:0.3]
	def loadAfter = ['mail']
	def observe = ['controllers','services']

    // resources that are excluded from plugin packaging
    def pluginExcludes = [
            "grails-app/views/error.gsp"
    ]

    // TODO Fill in these fields
    def author = "Mike Wille"
    def authorEmail = "mike.wille@flowz.com"
    def title = "Provides integration with Postmark mail service"
    def description = '''\\
This plugin provides an API with almost exact parity to the mail plugin.  Installing this plugin
replaces the sendMail injected method with it's own.  Doing so allows use of Postmark without
changing any code in your app or in the plugins your app depends on.

It does not support attachments as Postmark doesn't provide that capability.  If attachments
are used, it will be logged as an error and continue.
'''

    // URL to the plugin's documentation
    def documentation = "http://grails.org/plugin/postmark"

    def doWithWebDescriptor = { xml ->
        // TODO Implement additions to web.xml (optional), this event occurs before 
    }

    def doWithSpring = {
        // TODO Implement runtime spring config (optional)
    }

    def doWithDynamicMethods = { ctx ->
        // TODO Implement registering dynamic methods to classes (optional)
    }

    def doWithApplicationContext = { applicationContext ->
		configureSendMail(application, applicationContext)
    }

    def onChange = { event ->
		configureSendMail(event.application, event.ctx)
    }

    def onConfigChange = { event ->
        // TODO Implement code that is executed when the project configuration changes.
        // The event is the same as for 'onChange'.
    }

	def configureSendMail(application, applicationContext) {
		def log = LogFactory.getLog(PostmarkGrailsPlugin.class);

		def manager = org.codehaus.groovy.grails.plugins.PluginManagerHolder.pluginManager
		if(manager?.hasGrailsPlugin("mail")) {
			log.warn "Replacing mail plugin's [sendMail] with postmark plugin's [sendMail]"
		}

	    application.controllerClasses*.metaClass*.sendMail = {Closure callable ->
			applicationContext.postmarkService.sendPostmarkMail(callable)
		}

		application.serviceClasses.each {
			if(it.metaClass?.getTheClass()?.name != applicationContext.mailService?.metaClass?.getTheClass()?.name && 
				it.metaClass?.getTheClass()?.name != applicationContext.postmarkService?.metaClass?.getTheClass()?.name) {
				it.metaClass*.sendMail = {Closure callable ->
					applicationContext.postmarkService.sendPostmarkMail(callable)
				}
			}
		}
	}

}
