/*
 * Copyright (c) 2005, salesforce.com, inc.
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided 
 * that the following conditions are met:
 * 
 *    Redistributions of source code must retain the above copyright notice, this list of conditions and the 
 *    following disclaimer.
 *  
 *    Redistributions in binary form must reproduce the above copyright notice, this list of conditions and 
 *    the following disclaimer in the documentation and/or other materials provided with the distribution. 
 *    
 *    Neither the name of salesforce.com, inc. nor the names of its contributors may be used to endorse or 
 *    promote products derived from this software without specific prior written permission.
 *  
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED 
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A 
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR 
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED 
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) 
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING 
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
 * POSSIBILITY OF SUCH DAMAGE.
 */
package com.salesforce.dataloader.action;

import org.apache.log4j.Logger;

import com.salesforce.dataloader.action.IAction;
import com.salesforce.dataloader.action.progress.ILoaderProgress;
import com.salesforce.dataloader.config.Config;
import com.salesforce.dataloader.controller.Controller;
import com.salesforce.dataloader.dao.*;
import com.salesforce.dataloader.exception.DataAccessObjectException;

/**
 * Custom Action class
 *
 * @author awarshavsky
 * @since 144
 */
public class CustomAction implements IAction {

    private static Logger logger = Logger.getLogger(CustomAction.class);
    private final ILoaderProgress monitor;
    private final Controller controller;
    private final DataAccessObject dao;

    /**
     *
     */
    public CustomAction(Controller controller, ILoaderProgress monitor) {
        super();
        this.controller = controller;
        this.monitor = monitor;
        this.dao = controller.getDao();
        // figure out which dao interfaces are supported
        String daoKind = "DataAccessObject";
        if(this.dao instanceof DataReader) {
            daoKind += "," + "DataReader";
        }
        if(this.dao instanceof DataWriter) {
            daoKind += "," + "DataWriter";
        }
        logger.info("Custom data access object is a: " + daoKind);
        monitor.beginTask("Creating a 1000-record CustomAction", 1000);
        monitor.doneSuccess("Created a 1000-record CustomAction");
    }

    /*
     * (non-Javadoc)
     * @see com.salesforce.dataloader.action.IAction#execute()
     */
    public void execute() throws DataAccessObjectException {
        logger.info("Custom action is being executed, operation name is: " + controller.getConfig().getString(Config.OPERATION));
        monitor.beginTask("Executing a 1000-record CustomAction", 1000);
        monitor.setSubTask("Execute");
        monitor.doneError("Testing end with failure of a a 1000-record CustomAction");
    }

}
