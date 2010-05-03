package grails.plugins.postmark

import groovyx.net.http.*
import static groovyx.net.http.Method.*

class PostmarkService {

    static transactional = false
	
	def groovyPagesTemplateEngine


    Map sendPostmarkMail(Closure callable) {
		def messageBuilder = new MapBuilder()
		
		callable.delegate = messageBuilder
		callable.resolveStrategy = Closure.DELEGATE_FIRST
        callable.call()

		def message = messageBuilder.createMessage()
		if(message) {
			def config = org.codehaus.groovy.grails.commons.ConfigurationHolder.config
			if (!config.grails.mail.disabled) {
                if (log.traceEnabled) log.trace("Sending mail re: [${message.subject}] from [${message.from}] to ${message.to*.toString()} ...")
       			message.to.each { to ->
					sendPostmarkMail(message.from, to, message.subject, message.text, message.html, message.tag, message.headers)
				}
                if (log.traceEnabled) log.trace("Sent mail re: [${message.subject}] from [${message.from}] to ${message.to*.toString()}...")
            }
            else
                log.error("Sending emails disabled by configuration option")


		}
        return message
    }

	def sendPostmarkMail(String from, String to, String subject, String text, String html, String tag, Map headers) {
		def config = org.codehaus.groovy.grails.commons.ConfigurationHolder.config
		def defaultFrom = config.postmark.defaultFrom
		
		if(to == "")
			throw new IllegalArgumentException("To (recipient) is a required field.")
			
		if(from == "" && !defaultFrom)
				throw new IllegalArgumentException("From  is a required field.")
		
		if(subject == "") 
			subject = "No Subject"
			
		if(text == "" && html == "")
			throw new IllegalArgumentException("One or both of text and html are required fields.")
			
		def url = "http://api.postmarkapp.com/email"
		
		def apiKey = config.postmark.apikey
		if(!apiKey) {
			log.error "Postmark API Key is not set.  See: postmark.apikey"
			throw new Exception("Postmark API Key is not set.  See: postmark.apikey");
		}
			
		def http = new HTTPBuilder(url)

		http.headers["X-Postmark-Server-Token"] = apiKey


		def message = [ "From" : from, "To" : to, "Subject" : subject]
		
		if(tag != "")
			message.put("Tag", tag)
			
		if(html != "")
			message.put("HtmlBody", html)
			
		if(text != "")
		 	message.put("TextBody", text)
		
		if(headers && headers.size() > 0)
			message.put("Headers", headers)

		def postBody = message as grails.converters.JSON
		log.debug("POST:\n" + postBody.toString())
		http.request(POST, ContentType.JSON) {
			body = postBody.toString()
			//body = [ to: params.recipient, msg: params.text ]
			requestContentType = ContentType.JSON

			response.success = { resp, json ->
				log.warn "Sent email:\n ${json}"
			}

			response.failure = { resp, json ->
				log.error "Unexpected error sending email: ${resp.statusLine.statusCode}: ${resp.statusLine.reasonPhrase}"
				log.error "Message: ${json}"
				throw new Exception("Remote Postmark server error: ${resp.statusLine.reasonPhrase}")
			}
		}
    }
}
