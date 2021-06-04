/**
 * Copyright (C) 2014 OpenTravel Alliance (info@opentravel.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opentravel.dex.tasks.repository;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.opentravel.common.DexFileException;
import org.opentravel.common.DexFileHandler;
import org.opentravel.common.OpenProjectProgressMonitor;
import org.opentravel.dex.controllers.DexStatusController;
import org.opentravel.dex.tasks.DexTaskBase;
import org.opentravel.dex.tasks.DexTaskException;
import org.opentravel.dex.tasks.TaskResultHandlerI;
import org.opentravel.model.OtmModelManager;

import java.io.File;

/**
 * A JavaFX opening a project file
 * 
 * @author dmh
 *
 */
public class OpenProjectFileTask extends DexTaskBase<File> {
    private static Log log = LogFactory.getLog( OpenProjectFileTask.class );

    private OtmModelManager modelMgr;
    private DexStatusController status;

    /**
     * Create a open project file task.
     * 
     * @param taskData - a file to open
     * @param handler - results handler
     * @param status - a status controller that can post message and progress indicator
     */
    public OpenProjectFileTask(File taskData, OtmModelManager modelMgr, TaskResultHandlerI handler,
        DexStatusController status) {
        super( taskData, handler, status );
        this.modelMgr = modelMgr;
        this.status = status;

        if (taskData != null) {
            // Replace start message from super-type.
            msgBuilder = new StringBuilder( "Retrieving Libraries in Project: " );
            msgBuilder.append( taskData.getName() );
            updateMessage( msgBuilder.toString() );
        }
    }

    // Make sure only one thread is opening project at a time to prevent concurrency errors
    @Override
    public synchronized void doIT() throws DexTaskException {
        // log.debug( "Opening " + taskData.getName() );
        try {
            DexFileHandler.openProject( taskData, modelMgr, new OpenProjectProgressMonitor( status ) );
        } catch (DexFileException e) {
            throw new DexTaskException( e.getLocalizedMessage() );
        }
    }

}
