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

package com.salesforce.dataloader.client;

/**
 * The sfdc api client class - implemented using the partner wsdl
 *
 * @author lexiv
 * @since 140
 */

import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.beanutils.DynaBean;
import org.apache.log4j.Logger;

import com.salesforce.dataloader.config.Config;
import com.salesforce.dataloader.config.Messages;
import com.salesforce.dataloader.controller.Controller;
import com.salesforce.dataloader.dyna.SforceDynaBean;
import com.salesforce.dataloader.exception.ParameterLoadException;
import com.salesforce.dataloader.exception.PasswordExpiredException;
import com.sforce.soap.partner.Connector;
import com.sforce.soap.partner.DeleteResult;
import com.sforce.soap.partner.DescribeGlobalResult;
import com.sforce.soap.partner.DescribeGlobalSObjectResult;
import com.sforce.soap.partner.DescribeLayoutResult;
import com.sforce.soap.partner.DescribeSObjectResult;
import com.sforce.soap.partner.DescribeTabSetResult;
import com.sforce.soap.partner.Error;
import com.sforce.soap.partner.Field;
import com.sforce.soap.partner.GetDeletedResult;
import com.sforce.soap.partner.GetServerTimestampResult;
import com.sforce.soap.partner.GetUpdatedResult;
import com.sforce.soap.partner.GetUserInfoResult;
import com.sforce.soap.partner.LoginResult;
import com.sforce.soap.partner.MergeRequest;
import com.sforce.soap.partner.MergeResult;
import com.sforce.soap.partner.PartnerConnection;
import com.sforce.soap.partner.ProcessRequest;
import com.sforce.soap.partner.ProcessResult;
import com.sforce.soap.partner.QueryResult;
import com.sforce.soap.partner.ResetPasswordResult;
import com.sforce.soap.partner.SaveResult;
import com.sforce.soap.partner.SearchResult;
import com.sforce.soap.partner.SetPasswordResult;
import com.sforce.soap.partner.UndeleteResult;
import com.sforce.soap.partner.UpsertResult;
import com.sforce.soap.partner.fault.ApiFault;
import com.sforce.soap.partner.fault.ExceptionCode;
import com.sforce.soap.partner.sobject.SObject;
import com.sforce.ws.ConnectionException;
import com.sforce.ws.ConnectorConfig;

public class PartnerClient extends ClientBase<PartnerConnection> {

    PartnerConnection client;

   

    private DescribeGlobalResult entityTypes;
    private DescribeSObjectResult fieldTypes;
    private final Map<String, DescribeRefObject> referenceDescribes = new HashMap<String, DescribeRefObject>();
    private final Map<String, DescribeGlobalSObjectResult> describeGlobalResults = new HashMap<String, DescribeGlobalSObjectResult>();
    private final Map<String, DescribeSObjectResult> entityDescribes = new HashMap<String, DescribeSObjectResult>();

    private static Logger LOG = Logger.getLogger(PartnerClient.class);

    public PartnerClient(Controller controller) {
        super(controller, LOG);
    }

    public boolean connect() throws ApiFault, ConnectionException {
        return login();
    }

    @Override
    protected boolean connectPostLogin(ConnectorConfig cc) throws ConnectionException {
        this.client = Connector.newConnection(cc);

        this.client.setCallOptions(ClientBase.getClientName(this.config), null);
        // query header
        int querySize;
        try {
            querySize = config.getInt(Config.EXTRACT_REQUEST_SIZE);
        } catch (ParameterLoadException e) {
            querySize = Config.DEFAULT_EXTRACT_REQUEST_SIZE;
        }
        if (querySize > 0) {
            getClient().setQueryOptions(querySize);
        }

        // assignment rule for update
        if (config.getString(Config.ASSIGNMENT_RULE).length() > 14) { 
            String rule = config.getString(Config.ASSIGNMENT_RULE);
            if (rule.length() > 15) {
                rule = rule.substring(0, 15);
            }
            getClient().setAssignmentRuleHeader(rule, false);
        }

        // field truncation
        getClient().setAllowFieldTruncationHeader(config.getBoolean(Config.TRUNCATE_FIELDS));
        
        // TODO: make this configurable
        getClient().setDisableFeedTrackingHeader(true);
        return true;
    }

    public Date getServerTimestamp() throws ConnectionException {
        Date timestamp = null;

        try {
            if (!isSessionValid()) {
                connect();
            }
            GetServerTimestampResult gr;
            gr = getClient().getServerTimestamp();
            retries = 0;
            timestamp = gr.getTimestamp().getTime();
        } catch (ApiFault ex) {
            logger.error(Messages.getFormattedString("Client.operationError", new String[] {
                    "getServerTimestamp", ex.getExceptionMessage() }), ex); //$NON-NLS-1$
            // check retries
            if (doRetry(ex, "getServerTimestamp")) { return getServerTimestamp(); }
            throw ex;
        } catch (ConnectionException ex) {
            logger.error(Messages.getFormattedString(
                    "Client.operationError", new String[] { "getServerTimestamp", ex.getMessage() }), ex); //$NON-NLS-1$
            // check retries
            if (doRetry(ex, "getServerTimestamp")) { return getServerTimestamp(); }
            throw ex;
        }
        return timestamp;
    }

    public UpsertResult[] loadUpserts(List<DynaBean> dynaBeans) throws ConnectionException {
        logger.debug(Messages.getFormattedString("Client.beginOperation", "upsert")); //$NON-NLS-1$

        try {
            SObject[] sObjects = SforceDynaBean.getSObjectArray(controller, dynaBeans, config.getString(Config.ENTITY),
                    config.getBoolean(Config.INSERT_NULLS));

            if (!isSessionValid()) {
                connect();
            }

            logger.debug(Messages.getString("Client.arraySize") + sObjects.length); //$NON-NLS-1$

            UpsertResult[] ur;

            ur = getClient().upsert(config.getString(Config.EXTERNAL_ID_FIELD), sObjects);
            retries = 0;

            if (ur == null) logger.info(Messages.getString("Client.resultNull")); //$NON-NLS-1$
            for (int j = 0; j < ur.length; j++) {
                if (ur[j].getSuccess()) {
                    if (ur[j].getCreated()) {
                        logger.debug(Messages.getString("Client.itemCreated") + ur[j].getId()); //$NON-NLS-1$
                    } else {
                        logger.debug(Messages.getString("Client.itemUpdated") + ur[j].getId()); //$NON-NLS-1$
                    }
                }
                processResult(ur[j].getSuccess(), "Client.itemUpserted", ur[j].getId(), ur[j].getErrors(), j);
            }
            return ur;
        } catch (IllegalAccessException ex) {
            logger.error(Messages.getFormattedString(
                    "Client.operationError", new String[] { "upsert", ex.getMessage() }), ex); //$NON-NLS-1$
            throw new RuntimeException(ex);
        } catch (InvocationTargetException ex) {
            logger.error(Messages.getFormattedString(
                    "Client.operationError", new String[] { "upsert", ex.getMessage() }), ex); //$NON-NLS-1$
            throw new RuntimeException(ex);
        } catch (NoSuchMethodException ex) {
            logger.error(Messages.getFormattedString(
                    "Client.operationError", new String[] { "upsert", ex.getMessage() }), ex); //$NON-NLS-1$
            throw new RuntimeException(ex);
        } catch (ParameterLoadException ex) {
            logger.error(Messages.getFormattedString(
                    "Client.operationError", new String[] { "upsert", ex.getMessage() }), ex); //$NON-NLS-1$
            throw new RuntimeException(ex);
        } catch (ApiFault ex) {
            logger.error(Messages.getFormattedString(
                    "Client.operationError", new String[] { "upsert", ex.getExceptionMessage() }), ex); //$NON-NLS-1$
            // check retries
            if (doRetry(ex, "upsert")) { return loadUpserts(dynaBeans); }
            throw ex;
        } catch (ConnectionException ex) {
            logger.error(Messages.getFormattedString(
                    "Client.operationError", new String[] { "upsert", ex.getMessage() }), ex); //$NON-NLS-1$
            // check retries
            if (doRetry(ex, "upsert")) { return loadUpserts(dynaBeans); }
            throw ex;
        }
    }

    /**
     * @param dynaBeans
     * @return SaveResult array
     * @throws ConnectionException
     */
    public SaveResult[] loadUpdates(List<DynaBean> dynaBeans) throws ConnectionException {
        logger.debug(Messages.getFormattedString("Client.beginOperation", "update")); //$NON-NLS-1$

        try {
            SObject[] sObjects = SforceDynaBean.getSObjectArray(controller, dynaBeans, config.getString(Config.ENTITY),
                    config.getBoolean(Config.INSERT_NULLS));

            if (!isSessionValid()) {
                connect();
            }

            logger.debug(Messages.getString("Client.arraySize") + sObjects.length); //$NON-NLS-1$

            SaveResult[] sr = getClient().update(sObjects);
            retries = 0;

            if (sr == null) logger.info(Messages.getString("Client.resultNull")); //$NON-NLS-1$
            for (int j = 0; j < sr.length; j++) {
                processResult(sr[j].isSuccess(), "Client.itemUpdated", sr[j].getId(), sr[j].getErrors(), j);
            }
            return sr;
        } catch (IllegalAccessException ex) {
            logger.error(Messages.getFormattedString(
                    "Client.operationError", new String[] { "update", ex.getMessage() }), ex); //$NON-NLS-1$
            throw new RuntimeException(ex);
        } catch (InvocationTargetException ex) {
            logger.error(Messages.getFormattedString(
                    "Client.operationError", new String[] { "update", ex.getMessage() }), ex); //$NON-NLS-1$
            throw new RuntimeException(ex);
        } catch (NoSuchMethodException ex) {
            logger.error(Messages.getFormattedString(
                    "Client.operationError", new String[] { "update", ex.getMessage() }), ex); //$NON-NLS-1$
            throw new RuntimeException(ex);
        } catch (ParameterLoadException ex) {
            logger.error(Messages.getFormattedString(
                    "Client.operationError", new String[] { "update", ex.getMessage() }), ex); //$NON-NLS-1$
            throw new RuntimeException(ex);
        } catch (ApiFault ex) {
            logger.error(Messages.getFormattedString("Client.operationError", new String[] {
                    "update", ex.getExceptionMessage() }), ex); //$NON-NLS-1$
            // check retries
            if (doRetry(ex, "update")) { return loadUpdates(dynaBeans); }
            throw ex;
        } catch (ConnectionException ex) {
            logger.error(Messages.getFormattedString(
                    "Client.operationError", new String[] { "update", ex.getMessage() }), ex); //$NON-NLS-1$
            // check retries
            if (doRetry(ex, "update")) { return loadUpdates(dynaBeans); }
            throw ex;
        }
    }

    /**
     * @param dynaBeans
     * @return SaveResult array
     * @throws ConnectionException
     */
    public SaveResult[] loadInserts(List<DynaBean> dynaBeans) throws ConnectionException {
        logger.debug(Messages.getFormattedString("Client.beginOperation", "insert")); //$NON-NLS-1$
        try {
            SObject[] sObjects = SforceDynaBean.getSObjectArray(controller, dynaBeans, config.getString(Config.ENTITY),
                    config.getBoolean(Config.INSERT_NULLS));

            if (!isSessionValid()) {
                connect();
            }

            logger.debug(Messages.getString("Client.arraySize") + sObjects.length); //$NON-NLS-1$

            SaveResult[] sr = getClient().create(sObjects);
            retries = 0;

            if (sr == null) logger.info(Messages.getString("Client.resultNull")); //$NON-NLS-1$
            for (int j = 0; j < sr.length; j++) {
                processResult(sr[j].isSuccess(), "Client.itemCreated", sr[j].getId(), sr[j].getErrors(), j);
            }
            return sr;
        } catch (IllegalAccessException ex) {
            logger.error(Messages.getFormattedString(
                    "Client.operationError", new String[] { "insert", ex.getMessage() }), ex); //$NON-NLS-1$
            throw new RuntimeException(ex);
        } catch (InvocationTargetException ex) {
            logger.error(Messages.getFormattedString(
                    "Client.operationError", new String[] { "insert", ex.getMessage() }), ex); //$NON-NLS-1$
            throw new RuntimeException(ex);
        } catch (NoSuchMethodException ex) {
            logger.error(Messages.getFormattedString(
                    "Client.operationError", new String[] { "insert", ex.getMessage() }), ex); //$NON-NLS-1$
            throw new RuntimeException(ex);
        } catch (ParameterLoadException ex) {
            logger.error(Messages.getFormattedString(
                    "Client.operationError", new String[] { "insert", ex.getMessage() }), ex); //$NON-NLS-1$
            throw new RuntimeException(ex);
        } catch (ApiFault ex) {
            logger.error(Messages.getFormattedString("Client.operationError", new String[] {
                    "insert", ex.getExceptionMessage() }), ex); //$NON-NLS-1$
            // check retries
            if (doRetry(ex, "insert")) { return loadInserts(dynaBeans); }
            throw ex;
        } catch (ConnectionException ex) {
            logger.error(Messages.getFormattedString(
                    "Client.operationError", new String[] { "insert", ex.getMessage() }), ex); //$NON-NLS-1$
            // check retries
            if (doRetry(ex, "insert")) { return loadInserts(dynaBeans); }
            throw ex;
        }
    }

    /**
     * @param dynaBeans
     * @return DeleteResult array
     * @throws ConnectionException
     */
    public DeleteResult[] loadDeletes(List<DynaBean> dynaBeans) throws ConnectionException {
        logger.debug(Messages.getFormattedString("Client.beginOperation", "delete")); //$NON-NLS-1$
        try {

            DynaBean dynaBean;
            String[] dels = new String[dynaBeans.size()];
            for (int i = 0; i < dynaBeans.size(); i++) {
                dynaBean = dynaBeans.get(i);
                String id = (String)dynaBean.get("Id"); //$NON-NLS-1$
                if (id == null) {
                    id = "";
                }
                dels[i] = id;
            }

            if (!isSessionValid()) {
                connect();
            }

            logger.debug(Messages.getString("Client.arraySize") + dels.length); //$NON-NLS-1$

            DeleteResult[] sr = getClient().delete(dels);
            retries = 0;

            if (sr == null) logger.info(Messages.getString("Client.resultNull")); //$NON-NLS-1$
            for (int j = 0; j < sr.length; j++) {
                processResult(sr[j].isSuccess(), "Client.itemDeleted", sr[j].getId(), sr[j].getErrors(), j);
            }
            return sr;
        } catch (ApiFault ex) {
            logger.error(Messages.getFormattedString("Client.operationError", new String[] {
                    "delete", ex.getExceptionMessage() }), ex); //$NON-NLS-1$
            // check retries
            if (doRetry(ex, "delete")) { return loadDeletes(dynaBeans); }
            throw ex;
        } catch (ConnectionException ex) {
            logger.error(Messages.getFormattedString(
                    "Client.operationError", new String[] { "delete", ex.getMessage() }), ex); //$NON-NLS-1$
            // check retries
            if (doRetry(ex, "delete")) { return loadDeletes(dynaBeans); }
            throw ex;
        }
    }

    /**
     * @param dynaBeans
     * @return UndeleteResult array
     * @throws ConnectionException
     */
    public UndeleteResult[] loadUndeletes(List<DynaBean> dynaBeans) throws ConnectionException {
        logger.debug(Messages.getFormattedString("Client.beginOperation", "undelete")); //$NON-NLS-1$

        DynaBean dynaBean;
        String[] dels = new String[dynaBeans.size()];
        for (int i = 0; i < dynaBeans.size(); i++) {
            dynaBean = dynaBeans.get(i);
            dels[i] = (String)dynaBean.get("Id"); //$NON-NLS-1$
        }

        return undelete(dels);
    }

    /**
     * @param dynaBeans
     * @param recordToMergeIdsList
     * @return MargeResult array
     * @throws ConnectionException
     * @since 144
     */
    public MergeResult[] loadMerges(List<DynaBean> dynaBeans, List<String[]> recordToMergeIdsList)
            throws ConnectionException {
        logger.debug(Messages.getFormattedString("Client.beginOperation", "merge")); //$NON-NLS-1$
        try {
            if (!isSessionValid()) {
                connect();
            }

            MergeRequest[] mergeRequests = new MergeRequest[dynaBeans.size()];

            String entityName = config.getString(Config.ENTITY);
            for (int j = 0; j < mergeRequests.length; j++) {

                DynaBean dynaBean = dynaBeans.get(j);

                SObject sObj = SforceDynaBean.getSObject(controller, entityName, dynaBean);

                MergeRequest request = new MergeRequest();
                request.setMasterRecord(sObj);
                request.setRecordToMergeIds(recordToMergeIdsList.get(j));
                mergeRequests[j] = request;
            }

            MergeResult[] mr = getClient().merge(mergeRequests);
            retries = 0;
            if (mr == null) logger.info(Messages.getString("Client.resultNull")); //$NON-NLS-1$
            for (int j = 0; j < mr.length; j++) {
                processResult(mr[j].isSuccess(), "Client.itemMerged", mr[j].getId(), mr[j].getErrors(), j);
            }
            return mr;
        } catch (IllegalAccessException ex) {
            logger.error(Messages
                    .getFormattedString("Client.operationError", new String[] { "merge", ex.getMessage() }), ex); //$NON-NLS-1$
            // checked exceptions
            throw new RuntimeException(ex);
        } catch (InvocationTargetException ex) {
            logger.error(Messages
                    .getFormattedString("Client.operationError", new String[] { "merge", ex.getMessage() }), ex); //$NON-NLS-1$
            throw new RuntimeException(ex);
        } catch (NoSuchMethodException ex) {
            logger.error(Messages
                    .getFormattedString("Client.operationError", new String[] { "merge", ex.getMessage() }), ex); //$NON-NLS-1$
            throw new RuntimeException(ex);
        } catch (ParameterLoadException ex) {
            logger.error(Messages
                    .getFormattedString("Client.operationError", new String[] { "merge", ex.getMessage() }), ex); //$NON-NLS-1$
            throw new RuntimeException(ex);
        } catch (ApiFault ex) {
            logger.error(Messages.getFormattedString("Client.operationError", new String[] {
                    "merge", ex.getExceptionMessage() }), ex); //$NON-NLS-1$
            // check retries
            if (doRetry(ex, "merge")) { return loadMerges(dynaBeans, recordToMergeIdsList); }
            throw ex;
        } catch (ConnectionException ex) {
            logger.error(Messages
                    .getFormattedString("Client.operationError", new String[] { "merge", ex.getMessage() }), ex); //$NON-NLS-1$
            // check retries
            if (doRetry(ex, "merge")) { return loadMerges(dynaBeans, recordToMergeIdsList); }
            throw ex;
        }
    }

    /**
     * Query next batch of records using the query cursor
     *
     * @param ql
     * @return query results
     * @throws ConnectionException
     */
    public QueryResult queryMore(String ql) throws ConnectionException {
        logger.debug(Messages.getFormattedString("Client.beginOperation", "queryMore")); //$NON-NLS-1$
        try {
            if (!isSessionValid()) {
                connect();
            }
            QueryResult qr = getClient().queryMore(ql);
            retries = 0;
            return qr;
        } catch (ApiFault ex) {
            logger.error(Messages.getFormattedString("Client.operationError", new String[] {
                    "queryMore", ex.getExceptionMessage() }), ex); //$NON-NLS-1$
            // check retries
            if (doRetry(ex, "queryMore")) { return queryMore(ql); }
            throw ex;
        } catch (ConnectionException ex) {
            logger.error(Messages.getFormattedString(
                    "Client.operationError", new String[] { "queryMore", ex.getMessage() }), ex); //$NON-NLS-1$
            // check retries
            if (doRetry(ex, "queryMore")) { return queryMore(ql); }
            throw ex;
        }
    }

    /**
     * Query objects excluding the deleted objects
     *
     * @param soql
     * @return query results
     * @throws ConnectionException
     */
    public QueryResult query(String soql) throws ConnectionException {
        logger.debug(Messages.getFormattedString("Client.beginOperation", "query")); //$NON-NLS-1$
        try {
            if (!isSessionValid()) {
                connect();
            }
            QueryResult qr = getClient().query(soql);
            retries = 0;
            return qr;
        } catch (ApiFault ex) {
            logger.error(Messages.getFormattedString("Client.operationError", new String[] {
                    "query", ex.getExceptionMessage() }), ex); //$NON-NLS-1$
            // check retries
            if (doRetry(ex, "query")) { return query(soql); }
            throw ex;
        } catch (ConnectionException ex) {
            logger.error(Messages
                    .getFormattedString("Client.operationError", new String[] { "query", ex.getMessage() }), ex); //$NON-NLS-1$
            // check retries
            if (doRetry(ex, "query")) { return query(soql); }
            throw ex;
        }
    }

    /**
     * Query all the objects, including deleted ones
     *
     * @param SOQL
     * @return query results
     * @throws ConnectionException
     * @since 144
     */
    public QueryResult queryAll(String SOQL) throws ConnectionException {
        logger.debug(Messages.getFormattedString("Client.beginOperation", "queryAll")); //$NON-NLS-1$
        try {
            if (!isSessionValid()) {
                connect();
            }
            QueryResult qr = getClient().queryAll(SOQL);
            retries = 0;
            return qr;

        } catch (ApiFault ex) {
            logger.error(Messages.getFormattedString("Client.operationError", new String[] {
                    "queryAll", ex.getExceptionMessage() }), ex); //$NON-NLS-1$
            // check retries
            if (doRetry(ex, "queryAll")) { return queryAll(SOQL); }
            throw ex;
        } catch (ConnectionException ex) {
            logger.error(Messages.getFormattedString(
                    "Client.operationError", new String[] { "queryAll", ex.getMessage() }), ex); //$NON-NLS-1$
            // check retries
            if (doRetry(ex, "queryAll")) { return queryAll(SOQL); }
            throw ex;
        }
    }

    /**
     * @param fieldsToOutput
     * @param ids
     * @return Array of retrieved SObjects
     * @throws ConnectionException
     */
    public SObject[] retrieve(String fieldsToOutput, String[] ids) throws ConnectionException {
        logger.debug(Messages.getFormattedString("Client.beginOperation", "retrieve")); //$NON-NLS-1$
        try {
            if (!isSessionValid()) {
                connect();
            }
            SObject[] sObjectArray = getClient().retrieve(fieldsToOutput, config.getString(Config.ENTITY), ids);
            retries = 0;
            return sObjectArray;
        } catch (ApiFault ex) {
            logger.error(Messages.getFormattedString("Client.operationError", new String[] {
                    "retrieve", ex.getExceptionMessage() }), ex); //$NON-NLS-1$
            // check retries
            if (doRetry(ex, "retrieve")) { return retrieve(fieldsToOutput, ids); }
            throw ex;
        } catch (ConnectionException ex) {
            logger.error(Messages.getFormattedString(
                    "Client.operationError", new String[] { "retrieve", ex.getMessage() }), ex); //$NON-NLS-1$
            // check retries
            if (doRetry(ex, "retrieve")) { return retrieve(fieldsToOutput, ids); }
            throw ex;
        }
    }

    /**
     * Undelete the set of objects specified by the ids array
     *
     * @param ids
     *            Array of object id's to undelete
     * @return UndeleteResult array
     * @throws ConnectionException
     * @since 144
     */
    public UndeleteResult[] undelete(String[] ids) throws ConnectionException {
        logger.debug(Messages.getFormattedString("Client.beginOperation", "undelete")); //$NON-NLS-1$
        try {
            if (!isSessionValid()) {
                connect();
            }
            logger.debug(Messages.getString("Client.arraySize") + ids.length); //$NON-NLS-1$

            UndeleteResult[] ur = getClient().undelete(ids);
            retries = 0;

            if (ur == null) logger.info(Messages.getString("Client.resultNull")); //$NON-NLS-1$
            for (int j = 0; j < ur.length; j++) {
                processResult(ur[j].isSuccess(), "Client.itemUndeleted", ur[j].getId(), ur[j].getErrors(), j);
            }
            return ur;
        } catch (ApiFault ex) {
            logger.error(Messages.getFormattedString("Client.operationError", new String[] {
                    "undelete", ex.getExceptionMessage() }), ex); //$NON-NLS-1$
            // check retries
            if (doRetry(ex, "undelete")) { return undelete(ids); }
            throw ex;
        } catch (ConnectionException ex) {
            logger.error(Messages.getFormattedString(
                    "Client.operationError", new String[] { "undelete", ex.getMessage() }), ex); //$NON-NLS-1$
            // check retries
            if (doRetry(ex, "undelete")) { return undelete(ids); }
            throw ex;
        }
    }

    /**
     * @param sosl
     * @return Map of results - Lists of SObjects, indexed by SObject type
     * @throws ConnectionException
     */
    public SearchResult search(String sosl) throws ConnectionException {
        logger.debug(Messages.getFormattedString("Client.beginOperation", "search")); //$NON-NLS-1$
        try {
            if (!isSessionValid()) {
                connect();
            }
            SearchResult sr = getClient().search(sosl);

            retries = 0;

            return sr;
        } catch (ApiFault ex) {
            logger.error(Messages.getFormattedString("Client.operationError", new String[] {
                    "search", ex.getExceptionMessage() }), ex); //$NON-NLS-1$
            // check retries
            if (doRetry(ex, "search")) { return search(sosl); }
            throw ex;
        } catch (ConnectionException ex) {
            logger.error(Messages.getFormattedString(
                    "Client.operationError", new String[] { "search", ex.getMessage() }), ex); //$NON-NLS-1$
            // check retries
            if (doRetry(ex, "search")) { return search(sosl); }
            throw ex;
        }
    }

    /**
     * @param startDate
     * @param endDate
     * @return GetDeletedResult
     * @throws ConnectionException
     */
    public GetDeletedResult getDeleted(Calendar startDate, Calendar endDate) throws ConnectionException {
        logger.debug(Messages.getFormattedString("Client.beginOperation", "getDeleted")); //$NON-NLS-1$
        try {
            if (!isSessionValid()) {
                connect();
            }
            GetDeletedResult gdr = getClient().getDeleted(config.getString(Config.ENTITY), startDate, endDate);
            retries = 0;
            return gdr;
        } catch (ApiFault ex) {
            logger.error(Messages.getFormattedString("Client.operationError", new String[] {
                    "getDeleted", ex.getExceptionMessage() }), ex); //$NON-NLS-1$
            // check retries
            if (doRetry(ex, "getDeleted")) { return getDeleted(startDate, endDate); }
            throw ex;
        } catch (ConnectionException ex) {
            logger.error(Messages.getFormattedString(
                    "Client.operationError", new String[] { "getDeleted", ex.getMessage() }), ex); //$NON-NLS-1$
            // check retries
            if (doRetry(ex, "getDeleted")) { return getDeleted(startDate, endDate); }
            throw ex;
        }
    }

    /**
     * @param startDate
     * @param endDate
     * @return GetUpdatedResult
     * @throws ConnectionException
     */
    public GetUpdatedResult getUpdated(Calendar startDate, Calendar endDate) throws ConnectionException {
        logger.debug(Messages.getFormattedString("Client.beginOperation", "getUpdated")); //$NON-NLS-1$
        try {
            if (!isSessionValid()) {
                connect();
            }
            GetUpdatedResult gur = getClient().getUpdated(config.getString(Config.ENTITY), startDate, endDate);
            retries = 0;
            return gur;
        } catch (ApiFault ex) {
            logger.error(Messages.getFormattedString("Client.operationError", new String[] {
                    "getUpdated", ex.getExceptionMessage() }), ex); //$NON-NLS-1$
            // check retries
            if (doRetry(ex, "getUpdated")) { return getUpdated(startDate, endDate); }
            throw ex;
        } catch (ConnectionException ex) {
            logger.error(Messages.getFormattedString(
                    "Client.operationError", new String[] { "getUpdated", ex.getMessage() }), ex); //$NON-NLS-1$
            // check retries
            if (doRetry(ex, "getUpdated")) { return getUpdated(startDate, endDate); }
            throw ex;
        }
    }

    /**
     * @param objectToDescribe
     * @param recordTypeIds
     * @return DescribeLayoutResult
     * @throws ConnectionException
     */
    public DescribeLayoutResult describeLayout(String objectToDescribe, String[] recordTypeIds)
            throws ConnectionException {
        try {
            if (!isSessionValid()) {
                connect();
            }

            DescribeLayoutResult dlr = getClient().describeLayout(objectToDescribe, recordTypeIds);
            retries = 0;
            return dlr;
        } catch (ApiFault ex) {
            logger.error(Messages.getFormattedString("Client.operationError", new String[] {
                    "describeLayout", ex.getExceptionMessage() }), ex); //$NON-NLS-1$
            // check retries
            if (doRetry(ex, "describeLayout")) { return describeLayout(objectToDescribe, recordTypeIds); }
            throw ex;
        } catch (ConnectionException ex) {
            logger.error(Messages.getFormattedString(
                    "Client.operationError", new String[] { "describeLayout", ex.getMessage() }), ex); //$NON-NLS-1$
            // check retries
            if (doRetry(ex, "describeLayout")) { return describeLayout(objectToDescribe, recordTypeIds); }
            throw ex;
        }
    }

    /**
     * Describe tabs available in the UI
     *
     * @return DescribeTabSetResult array
     * @throws ConnectionException
     */
    public DescribeTabSetResult[] describeTabs() throws ConnectionException {
        try {
            if (!isSessionValid()) {
                connect();
            }

            DescribeTabSetResult[] dtsr = getClient().describeTabs();
            retries = 0;
            return dtsr;
        } catch (ApiFault ex) {
            logger.error(Messages.getFormattedString("Client.operationError", new String[] {
                    "describeTabs", ex.getExceptionMessage() }), ex); //$NON-NLS-1$
            // check retries
            if (doRetry(ex, "describeTabs")) { return describeTabs(); }
            throw ex;
        } catch (ConnectionException ex) {
            logger.error(Messages.getFormattedString(
                    "Client.operationError", new String[] { "describeTabs", ex.getMessage() }), ex); //$NON-NLS-1$
            // check retries
            if (doRetry(ex, "describeTabs")) { return describeTabs(); }
            throw ex;
        }
    }

    /**
     * Get user info about the logged in user
     *
     * @return logged in user info
     * @throws ConnectionException
     */
    public GetUserInfoResult getUserInfo() throws ConnectionException {
        try {
            if (!isSessionValid()) {
                connect();
            }

            GetUserInfoResult guir = getClient().getUserInfo();
            retries = 0;
            return guir;
        } catch (ApiFault ex) {
            logger.error(Messages.getFormattedString("Client.operationError", new String[] {
                    "getUserInfo", ex.getExceptionMessage() }), ex); //$NON-NLS-1$
            // check retries
            if (doRetry(ex, "getUserInfo")) { return getUserInfo(); }
            throw ex;
        } catch (ConnectionException ex) {
            logger.error(Messages.getFormattedString(
                    "Client.operationError", new String[] { "getUserInfo", ex.getMessage() }), ex); //$NON-NLS-1$
            // check retries
            if (doRetry(ex, "getUserInfo")) { return getUserInfo(); }
            throw ex;
        }
    }

    /**
     * Reset password for the loggged in user to a system-generated value.
     *
     * @param userId
     * @return ResetPasswordResult which contains the new password
     * @throws ConnectionException
     */
    public ResetPasswordResult resetPassword(String userId) throws ConnectionException {
        try {
            if (!isSessionValid()) {
                connect();
            }

            ResetPasswordResult rpr = getClient().resetPassword(userId);
            retries = 0;
            return rpr;
        } catch (ApiFault ex) {
            logger.error(Messages.getFormattedString("Client.operationError", new String[] {
                    "resetPassword", ex.getExceptionMessage() }), ex); //$NON-NLS-1$
            // check retries
            if (doRetry(ex, "resetPassword")) { return resetPassword(userId); }
            throw ex;
        } catch (ConnectionException ex) {
            logger.error(Messages.getFormattedString(
                    "Client.operationError", new String[] { "resetPassword", ex.getMessage() }), ex); //$NON-NLS-1$
            // check retries
            if (doRetry(ex, "resetPassword")) { return resetPassword(userId); }
            throw ex;
        }
    }

    /**
     * Set password for the user in the active session
     *
     * @param userId
     * @param password
     * @return SetPasswordResult
     * @throws ConnectionException
     */
    public SetPasswordResult setPassword(String userId, String password) throws ConnectionException {
        try {
            if (!isSessionValid()) {
                connect();
            }

            SetPasswordResult spr = getClient().setPassword(userId, password);
            retries = 0;
            return spr;
        } catch (ApiFault ex) {
            logger.error(Messages.getFormattedString("Client.operationError", new String[] {
                    "setPassword", ex.getExceptionMessage() }), ex); //$NON-NLS-1$
            // check retries
            if (doRetry(ex, "setPassword")) { return setPassword(userId, password); }
            throw ex;
        } catch (ConnectionException ex) {
            logger.error(Messages.getFormattedString(
                    "Client.operationError", new String[] { "setPassword", ex.getMessage() }), ex); //$NON-NLS-1$
            // check retries
            if (doRetry(ex, "setPassword")) { return setPassword(userId, password); }
            throw ex;
        }
    }

    /**
     * Submit actions of type ProcessRequest to the workflow engine on the server. Currently supported:
     * ProcessSubmitRequest, ProcessWorkitemRequest
     *
     * @param actions
     * @return ProcessResult array
     * @throws ConnectionException
     * @since 144
     */
    public ProcessResult[] process(ProcessRequest[] actions) throws ConnectionException {
        try {
            if (!isSessionValid()) {
                connect();
            }

            ProcessResult[] pr = getClient().process(actions);
            retries = 0;
            return pr;
        } catch (ApiFault ex) {
            logger.error(Messages.getFormattedString("Client.operationError", new String[] {
                    "process", ex.getExceptionMessage() }), ex); //$NON-NLS-1$
            // check retries
            if (doRetry(ex, "process")) { return process(actions); }
            throw ex;
        } catch (ConnectionException ex) {
            logger.error(Messages.getFormattedString(
                    "Client.operationError", new String[] { "process", ex.getMessage() }), ex); //$NON-NLS-1$
            // check retries
            if (doRetry(ex, "process")) { return process(actions); }
            throw ex;
        }
    }

    /**
     * Process result of a change operation that returns data success / errors (examples of operations with such
     * results: insert, update, upsert, delete, merge)
     *
     * @param success
     *            True if result is success
     * @param successMsgKey
     * @param id
     *            Item id(if available)
     * @param errors
     *            Error array(if available)
     * @param itemNbr
     *            Item number in the result
     */
    private void processResult(boolean success, String successMsgKey, String id, Error[] errors, int itemNbr) {
        if (success) {
            logger.debug(Messages.getString(successMsgKey) + id);
        } else {
            // there were errors during the delete call, go through the errors
            // array and write them to the screen
            for (Error err : errors) {
                int startRow;
                try {
                    startRow = config.getInt(Config.LOAD_ROW_TO_START_AT);
                } catch (ParameterLoadException e) {
                    startRow = 0;
                }
                logger.error(Messages.getString("Client.itemError") //$NON-NLS-1$
                        + new Integer((itemNbr + startRow)).toString());
                logger.error(Messages.getString("Client.errorCode") + err.getStatusCode().toString()); //$NON-NLS-1$
                logger.error(Messages.getString("Client.errorMessage") + err.getMessage()); //$NON-NLS-1$
            }
        }
    }

    @Override
    public PartnerConnection getClient() {
        return this.client;
    }

    public Map<String, DescribeGlobalSObjectResult> getDescribeGlobalResults() {
        return describeGlobalResults;
    }

    Map<String, DescribeSObjectResult> getEntityDescribeMap() {
        return this.entityDescribes;
    }

    DescribeGlobalResult getEntityTypes() {
        return entityTypes;
    }

    public DescribeSObjectResult getFieldTypes() {
        return fieldTypes;
    }

    public Map<String, DescribeRefObject> getReferenceDescribes() {
        return referenceDescribes;
    }

    boolean isSessionValid() {
        if (config.getBoolean(Config.SFDC_INTERNAL) && config.getBoolean(Config.SFDC_INTERNAL_IS_SESSION_ID_LOGIN)) { return true; }
        return isLoggedIn();
    }

    protected boolean login() throws ConnectionException, ApiFault {
        disconnect();
        // Attempt the login giving the user feedback
        logger.info(Messages.getString("Client.sforceLogin")); //$NON-NLS-1$
        ConnectorConfig cc = getLoginConnectorConfig();
        PartnerConnection conn = Connector.newConnection(cc);
        String server;
        try {
            // identify the client as dataloader
            conn.setCallOptions(ClientBase.getClientName(this.config), null);

            assert cc.isManualLogin();
            logger.info(Messages.getFormattedString(
                    "Client.sforceLoginDetail", new String[] { cc.getAuthEndpoint(), cc.getUsername() })); //$NON-NLS-1$

            if (config.getBoolean(Config.SFDC_INTERNAL) && config.getBoolean(Config.SFDC_INTERNAL_IS_SESSION_ID_LOGIN)) {
                conn.setSessionHeader(config.getString(Config.SFDC_INTERNAL_SESSION_ID));
                server = getServerUrl(config.getString(Config.ENDPOINT));
                conn.getUserInfo(); // check to make sure we have a good
                // connection
            } else {
                LoginResult loginResult = conn.login(cc.getUsername(), cc.getPassword());
                // if password has expired, throw an exception
                if (loginResult.getPasswordExpired()) { throw new PasswordExpiredException(Messages
                        .getString("Client.errorExpiredPassword")); //$NON-NLS-1$
                }
                // update session id and service endpoint based on response
                conn.setSessionHeader(loginResult.getSessionId());
                String serverUrl = loginResult.getServerUrl();
                server = getServerUrl(serverUrl);
                if (config.getBoolean(Config.RESET_URL_ON_LOGIN)) {
                    cc.setServiceEndpoint(serverUrl);
                }
            }

            loginSuccess(conn, server);
            return true;
        } catch (ApiFault ex) {
            logger.error(Messages.getFormattedString("Client.loginError", new String[] { cc.getAuthEndpoint(),
                    ex.getExceptionMessage() }), ex); //$NON-NLS-1$
            // check retries
            if (doRetry(ex, "login")) { return login(); }
            throw ex;
        } catch (ConnectionException ex) {
            logger.error(Messages.getFormattedString("Client.loginError", new String[] { cc.getAuthEndpoint(),
                    ex.getMessage() }), ex); //$NON-NLS-1$
            // check retries
            if (doRetry(ex, "login")) { return login(); }
            throw ex;
        }
    }

    private void loginSuccess(PartnerConnection conn, String serv) {
        this.retries = 0;
        this.client = conn;
        setSession(conn.getSessionHeader().getSessionId(), serv);
    }

    private String getServerStringFromUrl(URL url) {
        return url.getProtocol() + "://" + url.getAuthority();
    }

    private String getServerUrl(String serverUrl) {
        if (config.getBoolean(Config.RESET_URL_ON_LOGIN)) {
            try {
                return getServerStringFromUrl(new URL(serverUrl));
            } catch (MalformedURLException e) {
                logger.fatal("Unexpected error", e);
                throw new RuntimeException(e);
            }
        } else {
            return getDefaultServer();
        }
    }

    public boolean logout() {
        try {
            PartnerConnection pc = getClient();
            if (pc != null) pc.logout();
        } catch (ConnectionException e) {
            // ignore
        } finally {
            disconnect();
        }
        return true;
    }

    public void disconnect() {
        clearSession();
        this.client = null;
    }

    /**
     * @param operationName
     */
    protected void retrySleep(String operationName) {
        int sleepSecs;
        try {
            sleepSecs = config.getInt(Config.MIN_RETRY_SLEEP_SECS);
        } catch (ParameterLoadException e1) {
            sleepSecs = Config.DEFAULT_MIN_RETRY_SECS;
        }
        // sleep between retries is based on the retry attempt #. Sleep for longer periods with each retry
        sleepSecs = sleepSecs + (retries * 10); // sleep for MIN_RETRY_SLEEP_SECS + 10, 20, 30, etc.

        logger.info(Messages.getFormattedString("Client.retryOperation", new String[] { Integer.toString(retries + 1),
                operationName, Integer.toString(sleepSecs) }));
        try {
            Thread.sleep(sleepSecs * 1000);
        } catch (InterruptedException e) { // ignore
        }
    }

    /**
     * Gets the sObject describes for all entities
     */
    public boolean setEntityDescribes() throws ConnectionException {
        if (entityTypes == null && !setEntityTypes()) return false;
        if (this.describeGlobalResults.isEmpty()) {
            for (DescribeGlobalSObjectResult res : entityTypes.getSobjects()) {
                if (res != null) this.describeGlobalResults.put(res.getName(), res);
            }
        }

        return true;
    }

    /**
     * Gets the available objects from the global describe
     */
    private boolean setEntityTypes() throws ConnectionException {
        try {
            if (!isSessionValid()) {
                connect();
            }

            entityTypes = getClient().describeGlobal();
            retries = 0;
            return true;

        } catch (ApiFault ex) {
            logger.error(Messages.getFormattedString("Client.operationError", new String[] {
                    "describeGlobal", ex.getExceptionMessage() }), ex); //$NON-NLS-1$
            // check retries
            if (doRetry(ex, "describeGlobal")) { return setEntityTypes(); }
            throw ex;
        } catch (ConnectionException ex) {
            logger.error(Messages.getFormattedString(
                    "Client.operationError", new String[] { "describeGlobal", ex.getMessage() }), ex); //$NON-NLS-1$
            // check retries
            if (doRetry(ex, "describeGlobal")) { return setEntityTypes(); }
            throw ex;
        }
    }

    /**
     * Set the map of references to object external id info for current entity
     *
     * @throws ConnectionException
     */
    public void setFieldReferenceDescribes() throws ConnectionException {
        referenceDescribes.clear();
        if (getDescribeGlobalResults().isEmpty()) {
            setEntityDescribes();
        }
        if (getFieldTypes() == null) {
            setFieldTypes();
        }
        if (getDescribeGlobalResults() != null) {
            Field[] entityFields = getFieldTypes().getFields();
            for (Field entityField : entityFields) {
                // upsert on references (aka foreign keys) is supported only
                // 1. When field has relationship is set and refers to exactly one object
                // 2. When field is either createable or updateable. If neither is true, upsert will never work for that
                // relationship.
                if (entityField.isCreateable() || entityField.isUpdateable()) {
                    String relationshipName = entityField.getRelationshipName();
                    String[] referenceTos = entityField.getReferenceTo();
                    if (referenceTos != null && referenceTos.length == 1 && referenceTos[0] != null
                            && relationshipName != null && relationshipName.length() > 0
                            && (entityField.isCreateable() || entityField.isUpdateable())) {

                        String refEntityName = referenceTos[0];

                        // make sure that the object is legal to upsert
                        Field[] refObjectFields = describeSObject(refEntityName).getFields();
                        Map<String, Field> refFieldInfo = new HashMap<String, Field>();
                        for (Field refField : refObjectFields) {
                            if (refField.isExternalId()) {
                                refField.setCreateable(entityField.isCreateable());
                                refField.setUpdateable(entityField.isUpdateable());
                                refFieldInfo.put(refField.getName(), refField);
                            }
                        }
                        if (!refFieldInfo.isEmpty()) {
                            DescribeRefObject describe = new DescribeRefObject(refEntityName, refFieldInfo);
                            referenceDescribes.put(relationshipName, describe);
                        }
                    }
                }
            }
        }
    }

    /**
     * Gets the sobject describe for the given entity
     *
     * @throws ConnectionException
     */
    public void setFieldTypes() throws ConnectionException {
        this.fieldTypes = describeSObject(config.getString(Config.ENTITY));
    }

    /**
     * @return true if loggedIn
     */
    public boolean isLoggedIn() {
    	return getSessionId() != null;
    }

    private ConnectorConfig getLoginConnectorConfig() throws ConnectionException {
        ConnectorConfig cc = getConnectorConfig();
        String serverUrl = getDefaultServer();
        cc.setAuthEndpoint(serverUrl + DEFAULT_AUTH_ENDPOINT_URL.getPath());
        cc.setServiceEndpoint(serverUrl + DEFAULT_AUTH_ENDPOINT_URL.getPath());
        cc.setManualLogin(true);
        return cc;
    }

    private String getDefaultServer() {
        String serverUrl = config.getString(Config.ENDPOINT);
        if (serverUrl == null || serverUrl.length() == 0) {
            serverUrl = getServerStringFromUrl(DEFAULT_AUTH_ENDPOINT_URL);
        }
        return serverUrl;
    }

    /**
     * This function returns the describe call for an sforce entity
     *
     * @return DescribeSObjectResult
     * @throws ConnectionException
     */

    DescribeSObjectResult describeSObject(String entity) throws ConnectionException {

        DescribeSObjectResult describeSObjectResult = getEntityDescribeMap().get(entity);
        if (describeSObjectResult != null) return describeSObjectResult;
        try {

            if (!isSessionValid()) {
                connect();
            }

            describeSObjectResult = getClient().describeSObject(entity);
            retries = 0;
        } catch (ApiFault ex) {
            logger.error(Messages.getFormattedString("Client.operationError", new String[] {
                    "describeSObject", ex.getExceptionMessage() }), ex); //$NON-NLS-1$
            // check retries
            if (doRetry(ex, "describeSObject")) { return describeSObject(entity); }
            throw ex;
        } catch (ConnectionException ex) {
            logger.error(Messages.getFormattedString(
                    "Client.operationError", new String[] { "describeSObject", ex.getMessage() }), ex); //$NON-NLS-1$
            // check retries
            if (doRetry(ex, "describeSObject")) { return describeSObject(entity); }
            throw ex;
        }
        if (describeSObjectResult != null) {
            getEntityDescribeMap().put(describeSObjectResult.getName(), describeSObjectResult);
        }
        return describeSObjectResult;
    }

    /**
     * Checks whether retry makes sense for the given exception and given the number of current vs. max retries. If
     * retry makes sense, then before returning, this method will put current thread to sleep before allowing another
     * retry.
     *
     * @param ex
     * @param operationName
     * @return true if retry should be executed for operation. false if there's no retry.
     */
    protected boolean doRetry(ConnectionException ex, String operationName) {
        String msg;
        if (ex instanceof ApiFault) {
            msg = ((ApiFault)ex).getExceptionMessage();
        } else {
            msg = ex.getMessage();
        }
        if (msg.toLowerCase().indexOf("connection reset") > -1
                || (ex instanceof ApiFault && ((ApiFault)ex).getExceptionCode() == ExceptionCode.SERVER_UNAVAILABLE)) {
            if (enableRetries) {
                if (retries < maxRetries) {
                    retrySleep(operationName);
                    retries++;
                    return true;
                } else {
                    return false;
                }
            }
        }
        return false;
    }

}
