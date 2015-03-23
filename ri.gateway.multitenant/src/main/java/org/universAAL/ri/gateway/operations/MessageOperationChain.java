/*******************************************************************************
 * Copyright 2014 Universidad Politécnica de Madrid UPM
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.universAAL.ri.gateway.operations;

import org.universAAL.middleware.rdf.ScopedResource;

/**
 * Security interface for checking if messages should be resent or accepted
 * locally.
 * 
 * @author amedrano
 * 
 */
public interface MessageOperationChain extends OperationChain {

    /**
     * Run the pertinent checks to allow or deny the given message.
     * 
     * @param message
     * @return
     */
    OperationResult check(ScopedResource message);

}