package org.intermine.web;

/*
 * Copyright (C) 2002-2004 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import javax.mail.Transport;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.InternetAddress;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionError;
import org.apache.struts.action.ActionMessages;
import org.apache.struts.action.ActionMessage;

import java.text.MessageFormat;
import java.util.Map;
import java.util.HashMap;
import java.util.Random;
import java.util.Properties;

/**
 * Action to handle button presses RequestPasswordForm
 * @author Mark Woodbridge
 */
public class RequestPasswordAction extends Action
{
    protected Random random = new Random();

    /** 
     * Method called when user has finished updating a constraint
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception if the application business logic throws
     *  an exception
     */
    public ActionForward execute(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        ServletContext servletContext = session.getServletContext();
        ProfileManager pm = (ProfileManager) servletContext.getAttribute(Constants.PROFILE_MANAGER);
        Map webProperties = (Map) servletContext.getAttribute(Constants.WEB_PROPERTIES);
        String username = ((RequestPasswordForm) form).getUsername();

        boolean successful = false;
        if (pm.hasProfile(username)) {
            successful = email(username, pm.getPassword(username), webProperties);
        } else {
            String password = generatePassword();
            successful = email(username, password, webProperties);
            if (successful) {
                pm.saveProfile(new Profile(pm, username, new HashMap(), new HashMap()));
                pm.setPassword(username, password);
            }
        }

        if (successful) {
            ActionMessages messages = new ActionMessages();
            messages.add(ActionMessages.GLOBAL_MESSAGE,
                         new ActionMessage("login.emailed", username));
            saveMessages(request, messages);
        } else {
            ActionErrors errors = new ActionErrors();
            errors.add(ActionErrors.GLOBAL_ERROR, new ActionError("login.invalidemail"));
            saveErrors(request, errors);
        }

        return mapping.findForward("login");
    }

    /**
     * Generate a random 8-letter String of lower-case characters
     * @return the String
     */
    protected String generatePassword() {
        String s = "";
        for (int i = 0; i < 8; i++) {
            s += (char) ('a' + random.nextInt(26));
        }
        return s;
    }

    /**
     * Email a password to an email address
     * @param to the address to send to 
     * @param password the password to send
     * @param webProperties properties such as the from address
     * @return true if sending was successful
     */
    protected boolean email(String to, String password, Map webProperties) {
        String host = (String) webProperties.get("mail.host");
        String from = (String) webProperties.get("mail.from");
        String subject = (String) webProperties.get("mail.subject");
        String text = (String) webProperties.get("mail.text");
        text = MessageFormat.format(text, new Object[] {password});
        try {
            Properties properties = System.getProperties();
            properties.put("mail.smtp.host", host);
            MimeMessage message = new MimeMessage(Session.getDefaultInstance(properties, null));
            message.setFrom(new InternetAddress(from));
            message.addRecipient(Message.RecipientType.TO, InternetAddress.parse(to, true)[0]);
            message.setSubject(subject);
            message.setText(text);
            Transport.send(message);
            return true;
        } catch (Exception e) {
            Logger.log(e);
            return false;
        }
    }
}
