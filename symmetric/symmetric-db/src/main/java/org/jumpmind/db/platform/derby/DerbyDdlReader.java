package org.jumpmind.db.platform.derby;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import org.jumpmind.db.IDatabasePlatform;
import org.jumpmind.db.model.Column;
import org.jumpmind.db.model.ForeignKey;
import org.jumpmind.db.model.IIndex;
import org.jumpmind.db.model.Table;
import org.jumpmind.db.model.TypeMap;
import org.jumpmind.db.platform.AbstractJdbcDdlReader;
import org.jumpmind.db.platform.DatabaseMetaDataWrapper;
import org.jumpmind.log.Log;

/*
 * Reads a database model from a Derby database.
 */
public class DerbyDdlReader extends AbstractJdbcDdlReader {

    public DerbyDdlReader(IDatabasePlatform platform, Log log) {
        super(log, platform);
    }

    @Override
    protected Column readColumn(DatabaseMetaDataWrapper metaData, Map<String,Object> values) throws SQLException {
        Column column = super.readColumn(metaData, values);
        String defaultValue = column.getDefaultValue();

        if (defaultValue != null) {
            // we check for these strings
            // GENERATED_BY_DEFAULT -> 'GENERATED BY DEFAULT AS IDENTITY'
            // AUTOINCREMENT: start 1 increment 1 -> 'GENERATED ALWAYS AS
            // IDENTITY'
            if ("GENERATED_BY_DEFAULT".equals(defaultValue)
                    || defaultValue.startsWith("AUTOINCREMENT:")) {
                column.setDefaultValue(null);
                column.setAutoIncrement(true);
            } else if (TypeMap.isTextType(column.getTypeCode())) {
                column.setDefaultValue(unescape(defaultValue, "'", "''"));
            }
        }
        return column;
    }

    @Override
    protected boolean isInternalForeignKeyIndex(Connection connection,
            DatabaseMetaDataWrapper metaData, Table table, ForeignKey fk, IIndex index) {
        return isInternalIndex(index);
    }

    @Override
    protected boolean isInternalPrimaryKeyIndex(Connection connection,
            DatabaseMetaDataWrapper metaData, Table table, IIndex index) {
        return isInternalIndex(index);
    }

    /*
     * Determines whether the index is an internal index, i.e. one created by
     * Derby.
     * 
     * @param index The index to check
     * 
     * @return <code>true</code> if the index seems to be an internal one
     */
    private boolean isInternalIndex(IIndex index) {
        String name = index.getName();

        // Internal names normally have the form "SQL051228005030780"
        if ((name != null) && name.startsWith("SQL")) {
            try {
                Long.parseLong(name.substring(3));
                return true;
            } catch (NumberFormatException ex) {
                // we ignore it
            }
        }
        return false;
    }
}
