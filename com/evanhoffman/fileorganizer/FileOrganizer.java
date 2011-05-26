package com.evanhoffman.fileorganizer;

import java.io.File;
import java.io.FileNotFoundException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import com.evanhoffman.messagedigest.SHA1;

/**
 * Moves a bunch of files from their current locations to an arbitrary
 * target directory.
 * @author <a href="mailto:evanhoffman@evanhoffman.com">Evan Hoffman</a>
 * @since 2007-03-28
 */
public abstract class FileOrganizer {

	protected static Logger logger = Logger.getLogger("something");

	private File sourceDir = null;
	private boolean recurseSubdirs = true;
	private List<File> workFiles = null;

	private boolean renameOnCollision = true;

	public FileOrganizer(File sourceDir, boolean recurse) {
		this.sourceDir = sourceDir;
		this.recurseSubdirs = recurse;
	}

	/**
	 * Does stuff.
	 *
	 */
	public void run() {
		workFiles = getFileList(sourceDir);
		moveFiles(workFiles);
		logger.info("Done at "+ new Date());
	}

	/**
	 * Builds a list of files that match the criteria specified in {@link #accept(File)},
	 * recursing into subdirectories {@link #recurseSubdirs} is set to true.
	 * @param dir The starting directory.
	 * @return 
	 */
	protected List<File> getFileList(File dir) {
		List<File> list = new LinkedList<File>();

		File dirList[] = dir.listFiles(); 
		for (File f : dirList) {
			if (f.isDirectory()) {
				if (recurseSubdirs) {
					//					logger.info("Recursing into subdirectory "+f);
					list.addAll(getFileList(f));
				}
			} else {
				if (accept(f)) {
					list.add(f);
					//					logger.debug("Added file "+f);
				}
			}
		}

		return list;
	}

	/**
	 * Moves the files from their current location to the target locations
	 * returned by {@link #getTargetDirForFile(File)}.
	 * @param files
	 */
	protected void moveFiles(Iterable<File> files) {
		int filesMoved = 0;
		int fileCollisions = 0;
		int hashCollisions = 0;
		try {
			for (File f : files) {
				File targetDir = getTargetDirForFile(f);
				String filePrefix = getPrefixForFile(f);
				if (targetDir != null) {
					if (targetDir.exists()) {
						if (!targetDir.isDirectory()){
							throw new RuntimeException("'"+f+"' exists but is not a directory");
						}
					} else {
						if (!targetDir.mkdirs()) {
							throw new RuntimeException("Unable to create directory: "+f);
						}
					}

					File targetFile = null;
					if (filePrefix != null) {
						targetFile = new File(targetDir,filePrefix + "." + f.getName());
					} else {
						targetFile = new File(targetDir,f.getName());
					}
					
					if (renameOnCollision) {
						if (targetFile.exists()) {
							byte[] sourceHash = SHA1.getSHA1(f);
							byte[] targetHash = SHA1.getSHA1(targetFile);
							if (Arrays.equals(sourceHash, targetHash)) {
								logger.info("Source and target files are the same (file: "+f.getName()+", hash: "+SHA1.byteArrayToHexString(sourceHash) +")");
								hashCollisions++;
							} else {
								String oldTargetFile = targetFile.getName();
								targetFile = getValidFilename(targetFile, 100);
								logger.warn("File "+oldTargetFile+" renamed to "+targetFile.getName() );
								fileCollisions++;
								
								if (!f.renameTo(targetFile)) {
									throw new RuntimeException("Unable to move "+f+" to "+targetFile);
								} else {
									filesMoved++;
								}

							}
						}
						//		logger.debug("Moved "+f.getName()+" to "+targetFile.getAbsolutePath());
						
					} else {
						if (targetFile.exists()) {
							logger.error("Attempting to move "+f+" to "+targetFile+", but target already exists!  Source size: "+f.length()+", target size: "+targetFile.length());
						} else {
							if (!f.renameTo(targetFile)) {
								throw new RuntimeException("Unable to move "+f+" to "+targetFile);
							} 
							logger.debug("Moved "+f.getName()+" to "+targetFile.getParent());

						}
					}
				} else {
					logger.info("Null directory returned for file "+f);
				}
			}


			logger.info("Successfully moved "+filesMoved+" files, "+hashCollisions+" files already existed (matching SHA-1 hashes), "+fileCollisions+" files " +
			" had conflicting names and were renamed."); 
		} catch (FileNotFoundException fe) {
			logger.error("File not found exception: "+fe.getLocalizedMessage(),fe);
			throw new RuntimeException(fe);
		}
	}

	private static final Pattern EXTENSION = Pattern.compile("(\\w+)\\.(\\w+)$");
	private static final NumberFormat nf = new DecimalFormat("00000");
	/**
	 * If the given filename represents a filename that already exists, return a 
	 * filename that is unique in the same directory.
	 * @param f
	 * @return
	 */
	synchronized private File getValidFilename(File f, int maxTries) {
		if (!f.exists()) {
			return f;
		}

		// abc.jpg, abc = filename, jpg = extension;
		Matcher m = EXTENSION.matcher(f.getName());
		String extension = null;
		String filename = null;
		if (m.matches()) {
			filename = m.group(1);
			extension = m.group(2);

			for (int i = 0; i < maxTries; i++) {
				String newFilename = filename + "." + nf.format(i) + "."+extension;
				File tryFile = new File (f.getParentFile(),newFilename);
				if (!tryFile.exists()) {
					//					logger.info("Found available filename "+tryFile+" for original filename "+f);
					return tryFile;
				}
			}
			throw new RuntimeException("Exhausted maxTries ("+maxTries+") and could not determine a unique filename (base was "+f+")");
		} else {
			// there's no extension to preserve, so add the filename at the end
			for (int i = 0; i < maxTries; i++) {
				String newFilename = f.getName() + "." + nf.format(i);
				File tryFile = new File (f.getParentFile(),newFilename);
				if (!tryFile.exists()) {
					//					logger.info("Found available filename "+tryFile+" for original filename "+f);
					return tryFile;
				}
			}
			throw new RuntimeException("Exhausted maxTries ("+maxTries+") and could not determine a unique filename (base was "+f+")");
		}
	}

	/**
	 * Returns the directory to which the specified file should be moved, or null
	 * if the file should not be moved.
	 * @param f The file to be evaluated.
	 * @return
	 */
	protected abstract File getTargetDirForFile(File f);

	/**
	 * Returns true if the specified file is a candidate to be moved,
	 * false if it should be ignored.
	 * @param f The file to be evaluated.
	 * @return
	 */
	protected abstract boolean accept(File f);

	/**
	 * If the file should be renamed when moved, specify the prefix for the filename here.
	 * This is intended to allow you to put the mod date in the filename itself.
	 * @param f
	 * @return
	 */
	protected abstract String getPrefixForFile(File f);

}
