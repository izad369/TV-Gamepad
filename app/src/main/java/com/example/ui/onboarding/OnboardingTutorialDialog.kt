package com.example.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.NavigateBefore
import androidx.compose.material.icons.filled.NavigateNext
import androidx.compose.material.icons.filled.SettingsRemote
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.CyberDarkBg
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonOrange

data class TutorialStep(
    val stepNumber: Int,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val iconColor: Color,
    val descriptionLines: List<String>,
    val highlightBox: String
)

@Composable
fun OnboardingTutorialDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentStepIndex by remember { mutableIntStateOf(0) }

    val steps = remember {
        listOf(
            TutorialStep(
                stepNumber = 1,
                title = "اتصال بدون نصب برنامه روی تلویزیون",
                subtitle = "از طریق بلوتوث مستقیم (Bluetooth HID)",
                icon = Icons.Default.Bluetooth,
                iconColor = NeonCyan,
                descriptionLines = listOf(
                    "۱. در تلویزیون هوشمند خود با کنترل وارد منوی تنظیمات شوید.",
                    "۲. به بخش «کنترل‌ها و لوازم جانبی» (Remotes & Accessories) بروید.",
                    "۳. گزینه «افزودن دستگاه» (Add Accessory / Pair) را انتخاب کنید.",
                    "۴. در گوشی خود بلوتوث را روشن کنید تا نام دسته در تلویزیون ظاهر شود و روی آن کلیک کنید."
                ),
                highlightBox = "🌟 ویژگی طلایی: تلویزیون گوشی شما را دقیقاً مثل یک دسته بازی فیزیکی می‌شناسد و نیازی به نصب هیچ برنامه‌ای روی تلویزیون نیست!"
            ),
            TutorialStep(
                stepNumber = 2,
                title = "آموزش دکمه‌های گیم‌پد بازی",
                subtitle = "کنترل کامل بازی‌های دو بعدی و سه بعدی",
                icon = Icons.Default.Gamepad,
                iconColor = NeonGreen,
                descriptionLines = listOf(
                    "🕹️ دارای دو آنالوگ استیک مستقل (دسته حرکت + دسته چرخش دوربین).",
                    "🎛️ دو پد جهت‌نمای بالا، پایین، چپ و راست (D-Pad) برای بازی‌های مبارزه‌ای و رترو.",
                    "🎮 دکمه‌های اکشن کامل شامل A, B, X, Y و دکمه اختصاصی Z.",
                    "🔘 کلیدهای تریگر و شانه L1, R1, L2, R2 برای گاز/ترمز و تیراندازی.",
                    "📳 لرزش لمسی (Haptic) و حسگر حرکتی ژیروسکوپ جهت فرمان ماشین!"
                ),
                highlightBox = "💡 می‌توانید در بالای صفحه دسته بازی، حالت زاویه گوشی (Tilt) یا استیک/D-Pad را به دلخواه جابه‌جا کنید."
            ),
            TutorialStep(
                stepNumber = 3,
                title = "کنترل کامل تلویزیون (TV Remote)",
                subtitle = "ریموت هوشمند برای منوها و فیلم‌ها",
                icon = Icons.Default.SettingsRemote,
                iconColor = NeonOrange,
                descriptionLines = listOf(
                    "↩️ دکمه‌های حیاتی بازگشت (Back) و صفحه اصلی (Home) با دسترسی سریع.",
                    "🔘 چرخ ناوبری جهات (بالا، پایین، چپ، راست) همراه با کلید مرکزی OK.",
                    "🔊 تنظیم و قطع صدا (Volume + / - / Mute) و جابه‌جایی کانال‌ها.",
                    "⏯️ کنترل کامل پخش ویدیو، توقف، جلو و عقب بردن فیلم.",
                    "🖱️ پد لمسی ماوس: با حرکت انگشت، نشانگر ماوس را روی تلویزیون کنترل کنید."
                ),
                highlightBox = "🎯 دکمه‌های Back و Home به‌صورت دوطرفه کدگذاری شده‌اند تا با همه تلویزیون‌های هوشمند بدون خطا کار کنند."
            ),
            TutorialStep(
                stepNumber = 4,
                title = "ارسال آسان فایل و برنامه به تلویزیون",
                subtitle = "انتقال فیلم، عکس و فایل نصبی APK",
                icon = Icons.Default.FileDownload,
                iconColor = NeonCyan,
                descriptionLines = listOf(
                    "۱. به تب «ارسال فایل» بروید و دکمه «انتخاب فایل» را بزنید.",
                    "۲. هر فایلی (برنامه نصبی APK، فیلم، آهنگ یا عکس) را انتخاب کنید.",
                    "۳. در تلویزیون، مرورگر وب (یا برنامه Downloader) را باز کنید.",
                    "۴. آدرس نمایش داده شده را وارد کنید یا کد QR را اسکن کنید تا فایل با حداکثر سرعت وای‌فای دانلود و نصب شود."
                ),
                highlightBox = "⚡ با نهایت سرعت شبکه محلی (Wi-Fi) و بدون نیاز به مصرف حجم اینترنت فایل‌ها منتقل می‌شوند!"
            )
        )
    }

    val currentStep = steps[currentStepIndex]

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            color = Color.Transparent
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.radialGradient(
                            listOf(Color(0xFF142036), CyberDarkBg),
                            radius = 1200f
                        )
                    )
                    .border(1.5.dp, Brush.horizontalGradient(listOf(NeonCyan, NeonGreen)), RoundedCornerShape(24.dp))
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header with Step Indicator
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "آموزش و راهنمای کامل",
                            color = NeonCyan,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )

                        // Step Dots
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            steps.forEachIndexed { idx, _ ->
                                Box(
                                    modifier = Modifier
                                        .size(if (idx == currentStepIndex) 10.dp else 8.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (idx == currentStepIndex) NeonCyan else Color.DarkGray
                                        )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Step Content
                    AnimatedContent(targetState = currentStep, label = "step_transition") { step ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Icon Circle
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(step.iconColor.copy(alpha = 0.15f))
                                    .border(1.5.dp, step.iconColor, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = step.icon,
                                    contentDescription = null,
                                    tint = step.iconColor,
                                    modifier = Modifier.size(38.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = step.title,
                                color = Color.White,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Black,
                                textAlign = TextAlign.Center
                            )

                            Text(
                                text = step.subtitle,
                                color = NeonGreen,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
                            )

                            // Instructions Box
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                                shape = RoundedCornerShape(16.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    step.descriptionLines.forEach { line ->
                                        Text(
                                            text = line,
                                            color = Color(0xFFE2E8F0),
                                            fontSize = 13.sp,
                                            lineHeight = 22.sp
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Highlight callout
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = step.iconColor.copy(alpha = 0.12f)),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, step.iconColor.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = step.highlightBox,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    lineHeight = 20.sp,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Bottom Navigation Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (currentStepIndex > 0) {
                            OutlinedButton(
                                onClick = { currentStepIndex-- },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("tutorial_prev_btn")
                            ) {
                                Icon(Icons.Default.NavigateBefore, contentDescription = null, tint = Color.White)
                                Text("مرحله قبل", color = Color.White, fontSize = 13.sp)
                            }
                        } else {
                            Spacer(modifier = Modifier.width(8.dp))
                        }

                        if (currentStepIndex < steps.size - 1) {
                            Button(
                                onClick = { currentStepIndex++ },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("tutorial_next_btn")
                            ) {
                                Text("مرحله بعد", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Icon(Icons.Default.NavigateNext, contentDescription = null, tint = Color.Black)
                            }
                        } else {
                            Button(
                                onClick = onDismiss,
                                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("tutorial_finish_btn")
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("متوجه شدم • شروع برنامه", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
