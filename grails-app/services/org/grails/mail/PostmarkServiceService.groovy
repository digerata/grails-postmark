package org.grails.mail

import groovyx.net.http.*
import static groovyx.net.http.Method.*

class PostmarkServiceService {

    static transactional = false
	
	def groovyPagesTemplateEngine
	
    def sendMail() {
		def messageBuilder = new MapBuilder()
		
		callable.delegate = messageBuilder
		callable.resolveStrategy = Closure.DELEGATE_FIRST
        callable.call()

		def message = messageBuilder.createMessage()
		if(message) {
			
			if (!config.grails.mail.disabled) {
                if (log.traceEnabled) log.trace("Sending mail re: [${mimeMsg.subject}] from [${mimeMsg.from}] to ${mimeMsg.getRecipients(Message.RecipientType.TO)*.toString()} ...")        
       			message.to.each { to ->
					sendPostmarkMail(to, message.subject, message.text, message.html, message.tag, message.headers)
				}
                if (log.traceEnabled) log.trace("Sent mail re: [${message.subject}] from [${message.from}] to ${message.getRecipients(Message.RecipientType.TO)*.toString()}")
            }
            else
                log.warn("Sending emails disabled by configuration option")


		}
        return message
    }

	def sendPostmarkMail(String to, String subject, String text, String html, String tag, Map headers) {
		def url = "http://api.postmarkapp.com/email"
		
		def apiKey = config.grails.postmark.apikey
		if(!apiKey)
			throw new Exception("Postmark API Key is not set.  See: grails.postmark.apikey");
			
		def http = new HTTPBuilder(url)

		http.headers["X-Postmark-Server-Token"] = apiKey


		def message = [ "From" : "bounceoff@flowz.com", "To" : to, "Subject" : subject, "Tag" : tag, "HtmlBody": html, "TextBody" : text]
		if(headers && headers.size() > 0)
			message.put("Headers", headers)

		def postBody = message as grails.converters.JSON

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
