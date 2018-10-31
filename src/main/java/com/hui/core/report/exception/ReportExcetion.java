package com.hui.core.report.exception;

/**
 * <b><code>ReportExcetion</code></b>
 * <p/>
 * Description:
 * <p/>
 * <b>Creation Time:</b> 2018/10/31 22:41.
 *
 * @author Hu Weihui
 */
public class ReportExcetion extends RuntimeException {

    private static final long serialVersionUID = 5907460895157931893L;

    private String errorCode;

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public ReportExcetion() {
        super();
    }

    public ReportExcetion(String message) {
        super(message);
    }

    public ReportExcetion(String message, Throwable cause) {
        super(message, cause);
    }

    public ReportExcetion(Throwable cause) {
        super(cause);
    }


}
