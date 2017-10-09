package com.ebsco.entarch.mappings.xml2jsonl;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;


/**
 *
 * @author hji
 *
 */
public class DplConstant {

	// Added Camel route components
	public static final String RT_COMP_POLL_STRATEGY = "dplBatchPoll";
	public static final String RT_COMP_MEM_REPO = "dplMemRepo";
	public static final String RT_COMP_MONGO = "dplMongo";
	public static final String RT_COMP_FILE_FILTER = "dplFileFilter";

	// Added Camel exchange properties
	public static final String EXCHANGE_PROP_DPL_TRASH = "DPL_TRASH";
	public static final String EXCHANGE_PROP_DPL_DELETION = "DPL_DEL";
	public static final String EXCHANGE_PROP_DPL_DELETION_ID = "DPL_DEL_ID";
	public static final String EXCHANGE_PROP_DPL_XML_UNESCAPE_DONE = "DPL_XML_UNESCAPE_DONE";
	public static final String EXCHANGE_PROP_DPL_ZIP = "DPL_ZIP";
	public static final String EXCHANGE_PROP_DPL_FILE_START = "DPL_FILE_START";
	public static final String EXCHANGE_PROP_DPL_FILE_FINISH = "DPL_FILE_FINISH";
	public static final String EXCHANGE_PROP_DPL_BODY_RAW = "DPL_BODY_RAW";
	public static final String EXCHANGE_PROP_DPL_BODY_MAPPING_RESULT = "DPL_BODY_MAPPING_RESULT";
	public static final String EXCHANGE_PROP_DPL_AN = "DPL_AN";
	public static final String EXCHANGE_PROP_DPL_FT_DONE = "DPL_FT_DONE";
	public static final String EXCHANGE_PROP_DPL_TOO_MANY_ERRORS = "DPL_TOO_MANY_ERRORS";
	public static final String EXCHANGE_PROP_DPL_TOO_MANY_WARNINGS = "DPL_TOO_MANY_WARNINGS";
	public static final String EXCHANGE_PROP_DPL_VALID_JSON = "DPL_VALID_JSON";
	public static final String EXCHANGE_PROP_DPL_INCLUDE_JSON = "DPL_INCLUDE_JSON";
	public static final String EXCHANGE_PROP_DPL_MARC_FILE_END = "DPL_MARC_FILE_END";
	public static final String EXCHANGE_PROP_DPL_EMPTY_BODY = "DPL_EMPTY_BODY";
	public static final String EXCHANGE_PROP_DPL_DEL_REC = "DPL_DEL_REC";
	public static final String EXCHANGE_PROP_DPL_FT_ERROR = "DPL_FT_ERROR";

	// header flat to indicate that XML has mixed content
	public static final String XML_HAS_MIXED_CONTENT = "XML_HAS_MIXED_CONTENT";

	// escape "<" and ">" inside of text node to preserve mixed content
	public static final String XML_ESCAPE_LEFT = "DPL_XML_ESCAPE_LEFT";
	public static final String XML_ESCAPE_RIGHT = "DPL_XML_ESCAPE_RIGHT";

	// escape & before convert EPMARC JSON to XML
	public static final String XML_ESCAPE_AMP = "DPL_XML_ESCAPE_AMP";

	// filtered content
	public static final String DPL_FILTERED_CONTENT = "EP...EP";

	// default xml token
	public static final String XML_DEFAULT_TOKEN = "record";

	// MongoDB default host port
	public static final int MONGO_DEFAULT_PORT = 27017;

	// MongoDB default collection name
	public static final String MONGO_DEFAULT_COLLECTION = "records";

	// MongoDB database for user permission
	public static final String MONGO_DB_ADMIN = "admin";

	// MongoDB database for vtest
	public static final String MONGO_DB_VTEST = "dpl_vtest";

	// MongoDB collection for dataset meta data (path and count)
	public static final String MONGO_COL_META = "meta";

	// common split method name
	public static final String METHOD_SPLIT = "split";

	// default CSV header prefix
	public static final String CSV_DEFAULT_HEADER = "Field";


	// default error message
	public static final String ERR_MSG_DEFAULT = "Unexpected error";

	// logstash correlation
	public static final String LOG_CATEGORY = "category";
	public static final String LOG_APPNAME = "appName";
	public static final String LOG_CID = "cid";

	// mapping service warning
	public static final String MAPPING_SERVICE_RESP_WARN = "EP-Warnings";

	// temporary folder prefix
	public static final String TMP_UNZIP_FOLDER_PREFIX = "dpl_unzip_root_";

	// meta table key names
	public static final String META_KEY_NAME = "name";
	public static final String META_KEY_MAX_COUNT_PER_RECORD = "maxCountPerRecord";
	public static final String META_KEY_TOTAL_COUNT = "totalCount";

	// file info
	public static final String EBSCO_FILE_INFO = "EBSCO_FILE_INFO";
	public static final String EBSCO_FILE_INFO_DELETION_ID = "deletionID";

	// headers coming back from mapping service
	public static final String SLS_MAPPING_HEADERS = "slsHeaders";
	public static final String SLS_MAPPING_HEADER_FT_TAG = "ftTag";
	public static final String SLS_MAPPING_HEADER_PDF = "pdf";
	public static final String SLS_MAPPING_HEADER_IMAGES = "images";
	public static final String SLS_MAPPING_HEADER_AUDIO = "audio";
	public static final String SLS_MAPPING_HEADER_VIDEO = "video";

	// EPMARC hidden full text tag
	public static final String EPMARC_FT_TAG_HIDDEN = "39";
	public static final String EPMARC_FT_TAG_VISIBLE = "32";

	// EHost HTML entities
	public static Set<String> EHOST_TAGS = new HashSet<String>();
	public static int EHOST_TAG_MAX_LEN = 0;
	static {
		EHOST_TAGS.addAll(Arrays.asList("tt", "i", "b", "small", "sub", "sup", "br"));
		for (String entity : EHOST_TAGS) {
			if (entity.length() > EHOST_TAG_MAX_LEN) {
				EHOST_TAG_MAX_LEN = entity.length();
			}
		}
	}

	//US106892
	/**
	 * Key to authorities map stored in Exchange header
	 */
	public static String AUTHORITY_LIST_KEY = "AuthoritiesList";

	/**
	 * Key for boolean value in Exchange Header if mapping is multi
	 */
	public static String IS_MULTI_AUTHORITY = "IsMultiAuthority";

	//US108798
	public static final String JOB_ID = "jobId";
	public static final String WORK_IDS = "workIDs";
	//public static final int MAX_FILES = 2000;
	public static final int ONE_MEG = 1024 * 1024;
	public static final int ONE_HUNDRED_MEG = 100 * ONE_MEG;
	public static final int FIVE_HUNDRED_MEG = 5 * ONE_HUNDRED_MEG;
	public static final String SERVER = "server";
	//IDs for tese mocks, US132998
	public static final String UPDATE_WORK_TABLES_ID = "updateWorkTables";
	public static final String MAPPING_ROUTE_ID = "mappingRoute";
	public static final String WORK_CONSUMER_CNT_ID = "workConsumerCnt";
	public static final String GET_WORK_CONSUMER_ID = "getWorkConsumer";
	public static final String PROCESS_FILES_ID = "processFiles";
	//US141140
	public static final String HOST_IP = "hostip";
	public static final String PID = "pid";

	//US141751,US132344,US118610
	/**
	 * FileLister output file name
	 */
	public static String FILE_LIST_TXT = "fileList.txt";

	//US145115
	public static final String NO_OUTPUT_RECORD = "No MARCXML output record created.";
	public static final String NO_INPUT_DATA = "No input data to process.";

	//US185813
	public static final String DPL_ROUTE_CONFIG = "dplRouteConfig";
}
