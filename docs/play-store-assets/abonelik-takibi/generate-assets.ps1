Add-Type -AssemblyName System.Drawing
Add-Type -AssemblyName System.Windows.Forms

$ErrorActionPreference = "Stop"
$OutDir = Split-Path -Parent $MyInvocation.MyCommand.Path

function New-Canvas($w, $h, $color) {
    $bmp = New-Object System.Drawing.Bitmap($w, $h, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $g.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::AntiAliasGridFit
    $g.Clear($color)
    return @($bmp, $g)
}

function Brush($hex) {
    return New-Object System.Drawing.SolidBrush([System.Drawing.ColorTranslator]::FromHtml($hex))
}

function Pen($hex, $width = 1) {
    return New-Object System.Drawing.Pen([System.Drawing.ColorTranslator]::FromHtml($hex), $width)
}

function Font($size, $style = "Regular") {
    $fontStyle = [System.Drawing.FontStyle]::Regular
    if ($style -eq "Bold") { $fontStyle = [System.Drawing.FontStyle]::Bold }
    return New-Object System.Drawing.Font("Segoe UI", $size, $fontStyle, [System.Drawing.GraphicsUnit]::Pixel)
}

function RoundedPath($x, $y, $w, $h, $r) {
    $path = New-Object System.Drawing.Drawing2D.GraphicsPath
    $d = $r * 2
    $path.AddArc($x, $y, $d, $d, 180, 90)
    $path.AddArc($x + $w - $d, $y, $d, $d, 270, 90)
    $path.AddArc($x + $w - $d, $y + $h - $d, $d, $d, 0, 90)
    $path.AddArc($x, $y + $h - $d, $d, $d, 90, 90)
    $path.CloseFigure()
    return $path
}

function FillRound($g, $x, $y, $w, $h, $r, $hex) {
    $b = Brush $hex
    if ($r -le 0) {
        $g.FillRectangle($b, $x, $y, $w, $h)
        $b.Dispose()
        return
    }
    $p = RoundedPath $x $y $w $h $r
    $g.FillPath($b, $p)
    $p.Dispose(); $b.Dispose()
}

function StrokeRound($g, $x, $y, $w, $h, $r, $hex, $width = 2) {
    $p = RoundedPath $x $y $w $h $r
    $pen = Pen $hex $width
    $g.DrawPath($pen, $p)
    $p.Dispose(); $pen.Dispose()
}

function DrawText($g, $text, $x, $y, $w, $h, $size, $hex, $style = "Regular", $align = "Near") {
    $font = Font $size $style
    $brush = Brush $hex
    $fmt = New-Object System.Drawing.StringFormat
    $fmt.Alignment = [System.Drawing.StringAlignment]::$align
    $fmt.LineAlignment = [System.Drawing.StringAlignment]::Near
    $rect = New-Object System.Drawing.RectangleF($x, $y, $w, $h)
    $g.DrawString($text, $font, $brush, $rect, $fmt)
    $fmt.Dispose(); $brush.Dispose(); $font.Dispose()
}

function DrawLogoMark($g, $x, $y, $s, $withBg = $true) {
    if ($withBg) {
        FillRound $g $x $y $s $s ([int]($s * 0.22)) "#00535B"
    }
    $cardX = $x + [int]($s * 0.23)
    $cardY = $y + [int]($s * 0.17)
    $cardW = [int]($s * 0.54)
    $cardH = [int]($s * 0.66)
    FillRound $g $cardX $cardY $cardW $cardH ([int]($s * 0.07)) "#FFFFFF"
    FillRound $g $cardX $cardY $cardW ([int]($s * 0.17)) ([int]($s * 0.07)) "#E9B85E"
    FillRound $g ($cardX + [int]($s * 0.10)) ($cardY + [int]($s * 0.29)) ([int]($s * 0.34)) ([int]($s * 0.045)) 8 "#B7DDE1"
    FillRound $g ($cardX + [int]($s * 0.10)) ($cardY + [int]($s * 0.41)) ([int]($s * 0.28)) ([int]($s * 0.045)) 8 "#DDE9EA"
    FillRound $g ($cardX + [int]($s * 0.10)) ($cardY + [int]($s * 0.53)) ([int]($s * 0.22)) ([int]($s * 0.045)) 8 "#DDE9EA"
    $pen = Pen "#00535B" ([int]($s * 0.055))
    $g.DrawLines($pen, [System.Drawing.Point[]]@(
        (New-Object System.Drawing.Point(($cardX + [int]($s * 0.15)), ($cardY + [int]($s * 0.46)))),
        (New-Object System.Drawing.Point(($cardX + [int]($s * 0.25)), ($cardY + [int]($s * 0.57)))),
        (New-Object System.Drawing.Point(($cardX + [int]($s * 0.44)), ($cardY + [int]($s * 0.36))))
    ))
    $pen.Dispose()
    FillRound $g ($x + [int]($s * 0.64)) ($y + [int]($s * 0.13)) ([int]($s * 0.18)) ([int]($s * 0.18)) ([int]($s * 0.09)) "#E9B85E"
}

function SavePng($bmp, $path) {
    $bmp.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
}

function DrawMiniPhone($g, $x, $y, $w, $h, $screenName) {
    FillRound $g $x $y $w $h 54 "#102A2D"
    FillRound $g ($x + 16) ($y + 20) ($w - 32) ($h - 40) 40 "#F8FAFA"
    DrawText $g "Abonelik Takibi" ($x + 36) ($y + 56) ($w - 72) 54 18 "#00535B" "Bold"
    FillRound $g ($x + 42) ($y + 120) ($w - 84) 110 24 "#00535B"
    DrawText $g $screenName ($x + 62) ($y + 140) ($w - 124) 34 20 "#FFFFFF" "Bold"
    DrawText $g "₺8.420" ($x + 62) ($y + 176) 180 42 30 "#FFFFFF" "Bold"
    FillRound $g ($x + 42) ($y + 260) ($w - 84) 86 20 "#FFFFFF"
    StrokeRound $g ($x + 42) ($y + 260) ($w - 84) 86 20 "#E4ECEE" 2
    FillRound $g ($x + 66) ($y + 286) 54 54 14 "#DAF2F4"
    DrawText $g "N" ($x + 66) ($y + 294) 54 54 24 "#00535B" "Bold" "Center"
    DrawText $g "Netflix" ($x + 140) ($y + 276) 120 30 18 "#172526" "Bold"
    DrawText $g "Abonelik • Aylık" ($x + 140) ($y + 306) 220 28 15 "#627477"
    DrawText $g "₺229" ($x + $w - 135) ($y + 286) 82 30 17 "#00535B" "Bold" "Far"
    FillRound $g ($x + 42) ($y + 370) ($w - 84) 86 20 "#FFFFFF"
    StrokeRound $g ($x + 42) ($y + 370) ($w - 84) 86 20 "#E4ECEE" 2
    FillRound $g ($x + 66) ($y + 396) 54 54 14 "#FFF3CD"
    DrawText $g "E" ($x + 66) ($y + 404) 54 54 24 "#895100" "Bold" "Center"
    DrawText $g "Elektrik" ($x + 140) ($y + 386) 120 30 18 "#172526" "Bold"
    DrawText $g "Yaklaşan ödeme" ($x + 140) ($y + 416) 220 28 15 "#627477"
    DrawText $g "₺680" ($x + $w - 135) ($y + 396) 82 30 17 "#00535B" "Bold" "Far"
}

function New-FeatureGraphic {
    $pair = New-Canvas 1024 500 ([System.Drawing.ColorTranslator]::FromHtml("#F8FAFA"))
    $bmp = $pair[0]; $g = $pair[1]
    $grad = New-Object System.Drawing.Drawing2D.LinearGradientBrush(
        (New-Object System.Drawing.Rectangle(0,0,1024,500)),
        [System.Drawing.ColorTranslator]::FromHtml("#00535B"),
        [System.Drawing.ColorTranslator]::FromHtml("#0A777B"),
        [System.Drawing.Drawing2D.LinearGradientMode]::ForwardDiagonal
    )
    $g.FillRectangle($grad, 0, 0, 1024, 500)
    $grad.Dispose()
    FillRound $g 52 54 130 130 30 "#FFFFFF"
    DrawLogoMark $g 70 72 94 $false
    DrawText $g "Abonelik Takibi" 218 80 440 58 42 "#FFFFFF" "Bold"
    DrawText $g "Faturalarını, aboneliklerini ve yaklaşan ödemelerini tek yerden takip et." 220 150 470 90 24 "#D9F3F5"
    FillRound $g 220 268 170 48 24 "#E9B85E"
    DrawText $g "Akıllı özet" 247 278 120 32 20 "#3E2600" "Bold"
    FillRound $g 410 268 180 48 24 "#D9F3F5"
    DrawText $g "Bulut senkron" 435 278 140 32 20 "#00535B" "Bold"
    DrawMiniPhone $g 680 20 250 460 "Aylık Özet"
    SavePng $bmp (Join-Path $OutDir "feature-graphic-1024x500.png")
    $g.Dispose()
}

function New-AppIcon {
    $pair = New-Canvas 512 512 ([System.Drawing.ColorTranslator]::FromHtml("#00535B"))
    $bmp = $pair[0]; $g = $pair[1]
    $grad = New-Object System.Drawing.Drawing2D.LinearGradientBrush(
        (New-Object System.Drawing.Rectangle(0,0,512,512)),
        [System.Drawing.ColorTranslator]::FromHtml("#00535B"),
        [System.Drawing.ColorTranslator]::FromHtml("#007E87"),
        [System.Drawing.Drawing2D.LinearGradientMode]::ForwardDiagonal
    )
    $g.FillRectangle($grad, 0, 0, 512, 512)
    $grad.Dispose()
    DrawLogoMark $g 72 64 368 $false
    SavePng $bmp (Join-Path $OutDir "app-icon-512.png")
    $g.Dispose()
}

function New-PhoneShot($index, $title, $subtitle, $screenName, $accent) {
    $pair = New-Canvas 1080 1920 ([System.Drawing.ColorTranslator]::FromHtml("#F8FAFA"))
    $bmp = $pair[0]; $g = $pair[1]
    FillRound $g 0 0 1080 1920 0 "#F8FAFA"
    FillRound $g 70 72 116 116 28 "#00535B"
    DrawLogoMark $g 84 86 88 $false
    DrawText $g $title 70 230 940 120 56 "#172526" "Bold"
    DrawText $g $subtitle 70 360 900 92 30 "#627477"
    FillRound $g 110 520 860 1250 70 "#102A2D"
    FillRound $g 142 560 796 1170 52 "#FFFFFF"
    DrawText $g "Abonelik Takibi" 190 620 410 46 32 "#00535B" "Bold"
    FillRound $g 190 700 700 180 34 $accent
    DrawText $g $screenName 230 730 500 42 30 "#FFFFFF" "Bold"
    DrawText $g "₺8.420,00" 230 780 350 70 48 "#FFFFFF" "Bold"
    FillRound $g 190 930 700 120 28 "#FFFFFF"
    StrokeRound $g 190 930 700 120 28 "#E4ECEE" 3
    FillRound $g 226 962 64 64 16 "#DAF2F4"
    DrawText $g "A" 226 972 64 64 28 "#00535B" "Bold" "Center"
    DrawText $g "Netflix Premium" 316 952 360 34 26 "#172526" "Bold"
    DrawText $g "Abonelik • Aylık" 316 990 360 30 22 "#627477"
    DrawText $g "₺229,99" 720 970 130 34 24 "#00535B" "Bold" "Far"
    FillRound $g 190 1080 700 120 28 "#FFFFFF"
    StrokeRound $g 190 1080 700 120 28 "#E4ECEE" 3
    FillRound $g 226 1112 64 64 16 "#FFF3CD"
    DrawText $g "E" 226 1122 64 64 28 "#895100" "Bold" "Center"
    DrawText $g "Elektrik faturası" 316 1102 360 34 26 "#172526" "Bold"
    DrawText $g "3 gün içinde" 316 1140 360 30 22 "#627477"
    DrawText $g "₺680" 720 1120 130 34 24 "#00535B" "Bold" "Far"
    FillRound $g 190 1240 700 250 30 "#F2F7F8"
    DrawText $g "Akıllı Finans Özeti" 230 1272 420 40 28 "#00535B" "Bold"
    DrawText $g "• Önümüzdeki ay tahmini toplam`n• Yaklaşan ödeme uyarıları`n• Kampanya eşleştirme" 230 1330 560 120 24 "#30484B"
    DrawText $g "Subscription Tracker" 70 1810 500 42 28 "#00535B" "Bold"
    SavePng $bmp (Join-Path $OutDir ("phone-screenshot-{0:00}.png" -f $index))
    $g.Dispose()
}

function New-TabletShot($index, $title, $subtitle) {
    $pair = New-Canvas 1200 1920 ([System.Drawing.ColorTranslator]::FromHtml("#F8FAFA"))
    $bmp = $pair[0]; $g = $pair[1]
    DrawText $g $title 82 90 1030 90 54 "#172526" "Bold"
    DrawText $g $subtitle 82 180 980 70 30 "#627477"
    FillRound $g 90 310 1020 1320 52 "#FFFFFF"
    StrokeRound $g 90 310 1020 1320 52 "#D9E4E6" 4
    DrawText $g "Abonelik Takibi" 150 370 420 48 34 "#00535B" "Bold"
    FillRound $g 150 455 430 185 32 "#00535B"
    FillRound $g 620 455 430 185 32 "#E9B85E"
    DrawText $g "Bu ay toplam" 190 490 300 34 28 "#D9F3F5"
    DrawText $g "₺8.420" 190 535 300 70 50 "#FFFFFF" "Bold"
    DrawText $g "Yaklaşan" 660 490 300 34 28 "#5A3900"
    DrawText $g "3 ödeme" 660 535 300 70 50 "#3E2600" "Bold"
    for ($i=0; $i -lt 5; $i++) {
        $y = 710 + ($i * 145)
        FillRound $g 150 $y 900 108 28 "#FFFFFF"
        StrokeRound $g 150 $y 900 108 28 "#E4ECEE" 3
        FillRound $g 185 ($y+24) 60 60 16 "#DAF2F4"
        DrawText $g @("N","S","Y","E","D")[$i] 185 ($y+33) 60 60 27 "#00535B" "Bold" "Center"
        DrawText $g @("Netflix Premium","Spotify Duo","YouTube Premium","Elektrik faturası","Doğalgaz")[$i] 270 ($y+22) 430 34 26 "#172526" "Bold"
        DrawText $g @("Abonelik • Aylık","Abonelik • Aylık","Abonelik • Aylık","Fatura • Yaklaşan","Fatura • Tahmini")[$i] 270 ($y+60) 430 30 21 "#627477"
        DrawText $g @("₺229,99","₺135","₺119,99","₺680","₺920")[$i] 850 ($y+40) 150 34 24 "#00535B" "Bold" "Far"
    }
    SavePng $bmp (Join-Path $OutDir ("tablet-screenshot-{0:00}.png" -f $index))
    $g.Dispose()
}

New-AppIcon
New-FeatureGraphic
New-PhoneShot 1 "Aboneliklerini tek ekranda gör" "Fatura, kira ve dijital servislerini düzenli takip et." "Aylık Özet" "#00535B"
New-PhoneShot 2 "Akıllı finans özeti" "Yaklaşan ödemeleri ve gelecek ay tahminini hızlıca gör." "Akıllı Özet" "#007E87"
New-PhoneShot 3 "Ödeme takvimi" "Hangi gün ne ödeyeceğini kaçırma." "Takvim" "#895100"
New-PhoneShot 4 "Kategori analizi" "Abonelik, fatura ve diğer giderlerini kategori bazında incele." "Kategoriler" "#006D77"
New-PhoneShot 5 "Google ile senkron" "Kayıtlarını hesabına bağla, cihaz değiştirince kaybetme." "Bulut Senkron" "#00535B"
New-PhoneShot 6 "Ülkeye göre servisler" "Netflix, Spotify, Hulu, TOD ve daha fazlası bölgeye göre önerilir." "Popüler Hizmetler" "#007E87"
New-TabletShot 1 "Tablet görünümü hazır" "Geniş ekranda aboneliklerini daha rahat takip et."
New-TabletShot 2 "Aylık giderlerini yönet" "Akıllı özet, takvim ve kategori analizi tek yerde."

Write-Host "Generated Play Store assets in $OutDir"
