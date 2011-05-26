package com.evanhoffman.fileorganizer;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.log4j.BasicConfigurator;

public class AviOrganizer extends FileOrganizer {

//	static DateFormat df = new SimpleDateFormat("yyyy/MM/dd");
	static DateFormat df = new SimpleDateFormat("yyyy/yyyy-MM/yyyy-MM-dd");
	static DateFormat dfPrefix = new SimpleDateFormat("yyyy-MM-dd");

	
	private File targetDir = null;

	public AviOrganizer(File sourceDir, File targetDir, boolean recurse) {
		super(sourceDir, recurse);
		this.targetDir = targetDir;
	}

	@Override
	protected File getTargetDirForFile(File f) {
		Date lastModified = new Date(f.lastModified());
		File newTargetDir = new File(targetDir,df.format(lastModified)+"_(Movie)");
		return newTargetDir;
	}
	
	@Override
	protected String getPrefixForFile(File f) {
		Date lastModified = new Date(f.lastModified());
		return dfPrefix.format(lastModified);
	}

	@Override
	protected boolean accept(File f) {
		if (f.getName().toLowerCase().endsWith(".avi")) {
			return true;
		}
		if (f.getName().toLowerCase().endsWith(".mov")) {
			return true;
		}
		return false;
	}

	public static void main(String args[]) {
		BasicConfigurator.configure();
		int sourceDir = 0, targetDir = 1;
		AviOrganizer ao = new AviOrganizer(new File(args[sourceDir]),
				new File(args[targetDir]),true);
		ao.run();
		
	}

}
