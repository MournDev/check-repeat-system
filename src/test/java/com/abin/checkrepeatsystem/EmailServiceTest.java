package com.abin.checkrepeatsystem;

import com.abin.checkrepeatsystem.user.service.Impl.EmailService;
import jakarta.annotation.Resource;
import jakarta.mail.MessagingException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;


/**
 * QQ邮箱发送测试（配置完成后必须执行）
 */
@SpringBootTest
@RunWith(SpringRunner.class)
public class EmailServiceTest {

    @Resource
    private EmailService emailService;

    @Test
    public void testSendQqEmail() throws MessagingException {
        // 测试参数：接收邮箱（可填自己的另一个邮箱）、主题、内容
        boolean result = emailService.sendNoticeEmail(
                "2760635675@qq.com",  // 接收人邮箱（测试用，填你能收到的邮箱）
                "QQ邮箱配置测试",     // 邮件主题
                "恭喜！QQ邮箱配置成功，这是一封测试邮件～\n\n系统通知功能已正常可用！"  // 邮件内容
        );
        // 断言：若发送成功，result为true；失败则抛出异常
        assert result : "QQ邮箱发送测试失败，请检查配置！";
        System.out.println("QQ邮箱测试邮件发送成功！");
    }
}
