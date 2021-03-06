/*******************************************************************************
 * Copyright 2011, 2012 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 * 
 * This file is part of SITools2.
 * 
 * SITools2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * SITools2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with SITools2.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package fr.cnes.sitools.dataset.database.mongodb;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.restlet.Context;

import com.mongodb.BasicDBObject;
import com.mongodb.MongoException;

import fr.cnes.sitools.common.exception.SitoolsException;
import fr.cnes.sitools.dataset.database.DatabaseRequestParameters;
import fr.cnes.sitools.dataset.database.Range;
import fr.cnes.sitools.datasource.mongodb.business.MongoDBRequestModel;
import fr.cnes.sitools.datasource.mongodb.business.SitoolsMongoDBDataSource;

/**
 * Execute a request for some records specified by a List of ranges
 * 
 * 
 * @author m.gond
 */
public class MongoDBRangeDatabaseRequest extends MongoDBDatabaseRequest {

  /** logger de la resource Context / Application ? ok */
  private Logger logger = Context.getCurrentLogger();

  /** The list of ranges to query */
  private List<Range> ranges;

  /** The number of the current range in the ranges List */
  private int currentRangeIndex = 0;

  /** The request to perform */
  private MongoDBRequestModel request;

  /** The nbTotalResults */
  private int nbTotalResults = 0;
  /** The datasource */
  private SitoolsMongoDBDataSource datasource = null;

  /**
   * Create a new SQLRangeDatabaseRequest with the given {@link DatabaseRequestParameters}
   * 
   * @param params
   *          the {@link DatabaseRequestParameters}
   */
  public MongoDBRangeDatabaseRequest(DatabaseRequestParameters params) {
    super(params);
    ranges = params.getRanges();
    datasource = (SitoolsMongoDBDataSource) params.getDb();
  }

  @Override
  public final void createRequest() throws SitoolsException {

    nbTotalResults = calculateTotalCount();
    // set is count done to false not to calculate the total number of records for the request
    params.setCountDone(false);

    request = createMongoDBRequestModel();

    executeRequest(ranges.get(currentRangeIndex));

  }

  @Override
  public void createDistinctRequest() throws SitoolsException {
    this.createRequest();
  }

  @Override
  public final boolean nextResult() throws SitoolsException {

    if (rs == null) {
      return false;
    }
    try {
      if (rs.hasNext()) {
        currentObject = (BasicDBObject) rs.next();
        return true;
      }
      else {
        currentRangeIndex++;
        if (currentRangeIndex < ranges.size()) {
          executeRequest(ranges.get(currentRangeIndex));
          currentObject = (BasicDBObject) rs.next();
          return true;
        }
        else {
          return false;
        }
      }
    }
    catch (MongoException e) {
      throw new SitoolsException(e.getMessage(), e);
    }
  }

  /**
   * Execute the SQL request for a particular range
   * 
   * @param range
   *          the range
   */
  private void executeRequest(Range range) {
    int limit = range.getEnd() - range.getStart() + 1;
    int offset = range.getStart();

    request.setLimit(limit);
    request.setStart(offset);

    this.logger.log(Level.INFO, "REQUEST WITH RANGE\nCOLLECTION = " + request.getCollectionName());
    this.logger.log(Level.INFO, "JSON = " + request.getFilterString());
    this.logger.log(Level.INFO, "KEYS : " + request.getKeysString());
    this.logger.log(Level.INFO, "SORT :" + request.getSortString());
    this.logger.log(Level.INFO, "START :" + request.getStart() + " LIMIT " + request.getLimit());

    rs = datasource.limitedQuery(request);

  }

  @Override
  public int getTotalCount() {
    return nbTotalResults;
  }

  @Override
  public int getCount() {
    return nbTotalResults;
  }

  /*
   * (non-Javadoc)
   * 
   * @see fr.cnes.sitools.dataset.jdbc.SQLDatabaseRequest#calculateTotalCountFromBase()
   */
  @Override
  public int calculateTotalCountFromBase() throws SitoolsException {
    return calculateTotalCount();
  }

  /**
   * Calculate the number of records for the list of ranges
   * 
   * @return the total number of records to query
   */
  private int calculateTotalCount() {
    int totalCount = 0;
    for (Range range : ranges) {
      totalCount += range.getEnd() - range.getStart() + 1;
    }
    return totalCount;
  }

}
