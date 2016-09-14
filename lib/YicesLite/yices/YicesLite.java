package yices;
/**
 * 
 */

/**
 * @author Sokharith Sok
 *
 */
public class YicesLite {
	public native void yicesl_set_verbosity(short l);
	public native String yicesl_version();
	public native void yicesl_enable_type_checker(short flag);
	public native void yicesl_enable_log_file(String filename);
	public native int yicesl_mk_context();
	public native void yicesl_del_context(int ctx);
	public native int yicesl_read(int ctx, String cmd);
	public native int yicesl_inconsistent(int ctx);
	public native String yicesl_get_last_error_message();
	public native void yicesl_set_output_file(String filename);
	static{
		System.loadLibrary("YicesLite");
	//	System.loadLibrary("yices");
	}
}
