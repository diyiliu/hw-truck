import com.tiza.plugin.util.CommonUtil;
import org.junit.Test;

/**
 * Description: TestMain
 * Author: DIYILIU
 * Update: 2019-07-02 09:19
 */
public class TestMain {

    @Test
    public void test(){
        String str = "810154495A413132333435363738393031323301001913070209291B018054495A4131323334353637383930333231";

        byte[] bytes = CommonUtil.hexStringToBytes(str);
        System.out.println(CommonUtil.getCheck(bytes));
    }
}
