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

package com.salesforce.dataloader.ui;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;

import com.salesforce.dataloader.action.progress.SWTProgressAdapter;
import com.salesforce.dataloader.controller.Controller;
import com.salesforce.dataloader.exception.DataAccessObjectException;
import com.salesforce.dataloader.exception.OperationException;
import com.salesforce.dataloader.exception.OperationInitializationException;

/**
 * @author Lexi
 *
 */
public class SWTLoadRunable{
    private final Controller controller;

  public SWTLoadRunable(Controller controller) {
    super();
    this.controller = controller;
  }

  /* (non-Javadoc)
   * @see org.eclipse.jface.operation.IRunnableWithProgress#run(org.eclipse.core.runtime.IProgressMonitor)
   */
  public void run(){
    SWTProgressAdapter adapter = new SWTProgressAdapter( controller);

        try {
			controller.executeAction(adapter);
		} catch (OperationInitializationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (DataAccessObjectException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (OperationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
   
  }

}
