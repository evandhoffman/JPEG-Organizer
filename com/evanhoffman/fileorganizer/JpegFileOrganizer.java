package com.evanhoffman.fileorganizer;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.log4j.BasicConfigurator;

import com.drew.imaging.jpeg.JpegMetadataReader;
import com.drew.imaging.jpeg.JpegProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.MetadataException;
import com.drew.metadata.exif.ExifDirectory;

/**
 * Moves JPEGs from their source directory to a directory based on their
 * EXIF creation dates.
 * @author <a href="mailto:evanhoffman@evanhoffman.com">Evan Hoffman</a>
 * @since 2007-03-28

 */
public class JpegFileOrganizer extends FileOrganizer {

	static String jpegExtensions[] = { ".jpg" , ".jpeg", ".jpe" };

	static DateFormat df = new SimpleDateFormat("yyyy/yyyy-MM/yyyy-MM-dd");
	static DateFormat dfPrefix = new SimpleDateFormat("yyyy-MM-dd");

	private File targetDir = null;

	public JpegFileOrganizer(File sourceDir, File targetDir, boolean recurse) {
		super(sourceDir, recurse);
		this.targetDir = targetDir;
	}

	/**
	 * @see <a href="http://www.drewnoakes.com/code/exif/sampleUsage.html">http://www.drewnoakes.com/code/exif/sampleUsage.html</a>
	 */
	@Override
	protected File getTargetDirForFile(File f) {
		try {
			Metadata metadata = JpegMetadataReader.readMetadata(f);
//			Iterator directories = metadata.getDirectoryIterator(); 
//			while (directories.hasNext()) { 
//			Directory directory = (Directory)directories.next(); 
//			// iterate through tags and print to System.out  
//			Iterator tags = directory.getTagIterator(); 
//			while (tags.hasNext()) { 
//			Tag tag = (Tag)tags.next(); 
//			// use Tag.toString()  
//			System.out.println(tag); 
//			} 
//			}			

//			JpegSegmentReader segmentReader = new JpegSegmentReader(f); 
//			byte[] exifSegment = segmentReader.readSegment(JpegSegmentReader.SEGMENT_APP1); 
//			byte[] iptcSegment = segmentReader.readSegment(JpegSegmentReader.SEGMENT_APPD); 
//			Metadata metadata = new Metadata(); 
//			new ExifReader(exifSegment).extract(metadata); 
//			new IptcReader(iptcSegment).extract(metadata);

			Directory exifDirectory = metadata.getDirectory(ExifDirectory.class); 
//			String cameraMake = exifDirectory.getString(ExifDirectory.TAG_MAKE); 
//			String cameraModel = exifDirectory.getString(ExifDirectory.TAG_MODEL);
			Date date = null;
			if (exifDirectory.containsTag(ExifDirectory.TAG_DATETIME)) {
				date = exifDirectory.getDate(ExifDirectory.TAG_DATETIME);
			}
			Date dateDigitized = null;
			if (exifDirectory.containsTag(ExifDirectory.TAG_DATETIME_DIGITIZED)) {
				dateDigitized = exifDirectory.getDate(ExifDirectory.TAG_DATETIME_DIGITIZED);
			}
			Date dateOriginal = null;
			if (exifDirectory.containsTag(ExifDirectory.TAG_DATETIME_ORIGINAL)) {
				exifDirectory.getDate(ExifDirectory.TAG_DATETIME_ORIGINAL);
			}

			String path = getPathFromDate(date,dateDigitized,dateOriginal);
			if (path != null) {
				return new File(targetDir,path);		
			} else {
				return null;
			}
		}
		catch (JpegProcessingException je) {
//			throw new RuntimeException(je);
			logger.error("Error processing file "+f+": "+je.getMessage(),je);
			return null;
		} catch (MetadataException me) {
			logger.error("Error processing file "+f+": "+me.getMessage(),me);
			return null;
//			throw new RuntimeException(me);
		}
	}

	@Override
	protected String getPrefixForFile(File f) {
		try {
			Metadata metadata = JpegMetadataReader.readMetadata(f);
			Directory exifDirectory = metadata.getDirectory(ExifDirectory.class); 
			Date date = null;
			if (exifDirectory.containsTag(ExifDirectory.TAG_DATETIME)) {
				date = exifDirectory.getDate(ExifDirectory.TAG_DATETIME);
			}
			Date dateDigitized = null;
			if (exifDirectory.containsTag(ExifDirectory.TAG_DATETIME_DIGITIZED)) {
				dateDigitized = exifDirectory.getDate(ExifDirectory.TAG_DATETIME_DIGITIZED);
			}
			Date dateOriginal = null;
			if (exifDirectory.containsTag(ExifDirectory.TAG_DATETIME_ORIGINAL)) {
				exifDirectory.getDate(ExifDirectory.TAG_DATETIME_ORIGINAL);
			}
			if (date != null) {
				return dfPrefix.format(date);
			}
			if (dateDigitized != null) {
				return dfPrefix.format(dateDigitized);
			}
			if (dateOriginal != null) {
				return dfPrefix.format(dateOriginal);
			}

		} 
		catch (JpegProcessingException je) {
//			throw new RuntimeException(je);
			logger.error("Error processing file "+f+": "+je.getMessage(),je);
			return null;
		} catch (MetadataException me) {
			logger.error("Error processing file "+f+": "+me.getMessage(),me);
			return null;
//			throw new RuntimeException(me);
		}


		return null;
	}

	static String getPathFromDate(Date d1, Date d2, Date d3) {
		if (d1 != null) {
			return df.format(d1);
		}
		if (d2 != null) {
			return df.format(d2);
		}
		if (d3 != null) {
			return df.format(d3);
		}
		return null;
//		throw new NullPointerException("All 3 dates were null");
	}

	@Override
	protected boolean accept(File f) {
		for (String ext : jpegExtensions) {
			if (f.getName().toLowerCase().endsWith(ext)) {
				return true;
			}
		}
		return false;
	}

	public static void main(String args[]) {
		BasicConfigurator.configure();
		int sourceDir = 0, targetDir = 1;
		JpegFileOrganizer jo = new JpegFileOrganizer(new File(args[sourceDir]),
				new File(args[targetDir]),true);
		jo.run();

	}
}
