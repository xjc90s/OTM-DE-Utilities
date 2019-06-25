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

package org.opentravel.dex.events;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javafx.event.EventTarget;
import javafx.event.EventType;

/**
 * OTM DEX event for signaling when a filter controller setting has changed.
 * 
 * @author dmh
 *
 */
public class DexFilterChangeEvent extends DexEvent {
    private static Log log = LogFactory.getLog( DexFilterChangeEvent.class );
    private static final long serialVersionUID = 20190409L;

    public static final EventType<DexFilterChangeEvent> FILTER_CHANGED = new EventType<>( DEX_ALL, "FILTER_CHANGED" );

    /**
     * Filter change event with no subject.
     */
    public DexFilterChangeEvent() {
        super( FILTER_CHANGED );
    }

    public DexFilterChangeEvent(Object source, EventTarget target) {
        super( source, target, FILTER_CHANGED );
        // log.debug("DexEvent source/target constructor ran.");
        // If there is data, extract it from source or target here
    }

}
