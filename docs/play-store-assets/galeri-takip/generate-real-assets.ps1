Add-Type -AssemblyName System.Drawing

$ErrorActionPreference = "Stop"
$OutDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$DownloadDir = Join-Path $env:USERPROFILE "Downloads"
$LogoPath = Join-Path $OutDir "stitch-galeri-takip-icon.png"

function Brush($hex) { New-Object System.Drawing.SolidBrush([System.Drawing.ColorTranslator]::FromHtml($hex)) }
function Pen($hex, $width = 1) { New-Object System.Drawing.Pen([System.Drawing.ColorTranslator]::FromHtml($hex), $width) }
function Font($size, $style = "Regular") {
    $fontStyle = [System.Drawing.FontStyle]::Regular
    if ($style -eq "Bold") { $fontStyle = [System.Drawing.FontStyle]::Bold }
    New-Object System.Drawing.Font("Segoe UI", $size, $fontStyle, [System.Drawing.GraphicsUnit]::Pixel)
}
function New-Canvas($w, $h, $hex) {
    $bmp = New-Object System.Drawing.Bitmap($w, $h, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::AntiAliasGridFit
    $g.Clear([System.Drawing.ColorTranslator]::FromHtml($hex))
    @($bmp, $g)
}
function RoundedPath($x, $y, $w, $h, $r) {
    $path = New-Object System.Drawing.Drawing2D.GraphicsPath
    $d = $r * 2
    $path.AddArc($x, $y, $d, $d, 180, 90)
    $path.AddArc($x + $w - $d, $y, $d, $d, 270, 90)
    $path.AddArc($x + $w - $d, $y + $h - $d, $d, $d, 0, 90)
    $path.AddArc($x, $y + $h - $d, $d, $d, 90, 90)
    $path.CloseFigure()
    $path
}
function FillRound($g, $x, $y, $w, $h, $r, $hex) {
    $brush = Brush $hex
    if ($r -le 0) { $g.FillRectangle($brush, $x, $y, $w, $h); $brush.Dispose(); return }
    $path = RoundedPath $x $y $w $h $r
    $g.FillPath($brush, $path)
    $path.Dispose(); $brush.Dispose()
}
function StrokeRound($g, $x, $y, $w, $h, $r, $hex, $width = 2) {
    $path = RoundedPath $x $y $w $h $r
    $pen = Pen $hex $width
    $g.DrawPath($pen, $path)
    $path.Dispose(); $pen.Dispose()
}
function DrawText($g, $text, $x, $y, $w, $h, $size, $hex, $style = "Regular", $align = "Near") {
    $font = Font $size $style
    $brush = Brush $hex
    $fmt = New-Object System.Drawing.StringFormat
    $fmt.Alignment = [System.Drawing.StringAlignment]::$align
    $fmt.LineAlignment = [System.Drawing.StringAlignment]::Near
    $fmt.Trimming = [System.Drawing.StringTrimming]::EllipsisWord
    $rect = New-Object System.Drawing.RectangleF($x, $y, $w, $h)
    $g.DrawString($text, $font, $brush, $rect, $fmt)
    $fmt.Dispose(); $brush.Dispose(); $font.Dispose()
}
function SavePng($bmp, $path) {
    $bmp.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
}
function DrawCar($g, $x, $y, $s) {
    $amber = "#F4B860"; $cream = "#FFF9F0"; $green = "#173F35"
    $body = New-Object System.Drawing.Drawing2D.GraphicsPath
    $body.AddPolygon([System.Drawing.Point[]]@(
        (New-Object System.Drawing.Point(($x + [int]($s*.15)), ($y + [int]($s*.56)))),
        (New-Object System.Drawing.Point(($x + [int]($s*.23)), ($y + [int]($s*.34)))),
        (New-Object System.Drawing.Point(($x + [int]($s*.34)), ($y + [int]($s*.25)))),
        (New-Object System.Drawing.Point(($x + [int]($s*.66)), ($y + [int]($s*.25)))),
        (New-Object System.Drawing.Point(($x + [int]($s*.77)), ($y + [int]($s*.34)))),
        (New-Object System.Drawing.Point(($x + [int]($s*.85)), ($y + [int]($s*.56)))),
        (New-Object System.Drawing.Point(($x + [int]($s*.85)), ($y + [int]($s*.73)))),
        (New-Object System.Drawing.Point(($x + [int]($s*.15)), ($y + [int]($s*.73))))
    ))
    $brush = Brush $amber; $g.FillPath($brush, $body); $brush.Dispose(); $body.Dispose()
    FillRound $g ($x + [int]($s*.29)) ($y + [int]($s*.37)) ([int]($s*.42)) ([int]($s*.15)) 10 $cream
    FillRound $g ($x + [int]($s*.24)) ($y + [int]($s*.59)) ([int]($s*.10)) ([int]($s*.10)) ([int]($s*.05)) $green
    FillRound $g ($x + [int]($s*.66)) ($y + [int]($s*.59)) ([int]($s*.10)) ([int]($s*.10)) ([int]($s*.05)) $green
}
function DrawLogo($g, $x, $y, $s) {
    if (Test-Path $LogoPath) {
        $logo = [System.Drawing.Image]::FromFile($LogoPath)
        $dst = New-Object System.Drawing.Rectangle($x, $y, $s, $s)
        $g.DrawImage($logo, $dst)
        $logo.Dispose()
    } else {
        FillRound $g $x $y $s $s ([int]($s*.24)) "#173F35"
        DrawCar $g ($x + [int]($s*.08)) ($y + [int]($s*.12)) ([int]($s*.84))
    }
}
function FillEllipseText($g, $x, $y, $w, $h, $text) {
    $brush = Brush "#050706"
    $g.FillEllipse($brush, $x, $y, $w, $h)
    $brush.Dispose()
    DrawText $g $text ($x+8) ($y+44) ($w-16) 42 18 "#FFFFFF" "Bold" "Center"
}
function Anonymize($img, $kind) {
    $g = [System.Drawing.Graphics]::FromImage($img)
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $g.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::AntiAliasGridFit
    if ($kind -eq "home" -or $kind -eq "report") { FillEllipseText $g 765 130 130 130 "GALERI" }
    if ($kind -eq "cash") {
        FillRound $g 86 1210 300 92 8 "#202421"; DrawText $g "Satış danışmanı" 88 1236 300 48 28 "#B8BDB9" "Regular"
        FillRound $g 86 1550 390 96 8 "#202421"; DrawText $g "Müşteri: Örnek müşteri" 88 1578 390 48 28 "#B8BDB9" "Regular"
    }
    if ($kind -eq "expense") {
        FillRound $g 88 1664 285 48 8 "#202421"; DrawText $g "Satış danışmanı" 88 1662 300 52 30 "#FFFFFF" "Bold"
    }
    if ($kind -eq "detail") {
        FillRound $g 88 144 190 48 8 "#202421"; DrawText $g "34 ABC 123" 88 140 210 52 28 "#B8BDB9" "Bold"
    }
    $g.Dispose()
}
function LoadShot($file, $kind) {
    $src = [System.Drawing.Image]::FromFile((Join-Path $DownloadDir $file))
    $bmp = New-Object System.Drawing.Bitmap($src.Width, $src.Height, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.DrawImage($src, 0, 0, $src.Width, $src.Height)
    $g.Dispose(); $src.Dispose()
    Anonymize $bmp $kind
    $bmp
}
function DrawImageCover($g, $img, $x, $y, $w, $h, $cropTop, $cropBottom) {
    $srcX = 0; $srcY = $cropTop; $srcW = $img.Width; $srcH = $img.Height - $cropTop - $cropBottom
    $targetRatio = $w / $h
    $srcRatio = $srcW / $srcH
    if ($srcRatio -gt $targetRatio) {
        $newW = [int]($srcH * $targetRatio)
        $srcX = [int](($srcW - $newW) / 2)
        $srcW = $newW
    } else {
        $newH = [int]($srcW / $targetRatio)
        $srcY = $srcY + [int](($srcH - $newH) / 2)
        $srcH = $newH
    }
    $srcRect = New-Object System.Drawing.Rectangle($srcX, $srcY, $srcW, $srcH)
    $dstRect = New-Object System.Drawing.Rectangle($x, $y, $w, $h)
    $g.DrawImage($img, $dstRect, $srcRect, [System.Drawing.GraphicsUnit]::Pixel)
}
function DrawPhoneMock($g, $img, $x, $y, $w, $h, $cropTop = 80, $cropBottom = 130) {
    FillRound $g $x $y $w $h 66 "#050706"
    FillRound $g ($x+18) ($y+18) ($w-36) ($h-36) 52 "#101512"
    $screenX = $x + 30; $screenY = $y + 34; $screenW = $w - 60; $screenH = $h - 68
    $clip = RoundedPath $screenX $screenY $screenW $screenH 38
    $state = $g.Save()
    $g.SetClip($clip)
    DrawImageCover $g $img $screenX $screenY $screenW $screenH $cropTop $cropBottom
    $g.Restore($state)
    $clip.Dispose()
    StrokeRound $g $screenX $screenY $screenW $screenH 38 "#26342E" 2
}
function DrawHeader($g, $title, $subtitle, $accent) {
    DrawLogo $g 72 62 98
    DrawText $g $title 72 188 920 164 54 "#FFFFFF" "Bold"
    DrawText $g $subtitle 72 354 900 82 29 "#D8E3DD"
    FillRound $g 72 422 230 46 23 $accent
    DrawText $g "Galeri Takip" 102 430 170 30 20 "#0D1512" "Bold" "Center"
}
function New-StoreShot($index, $file, $kind, $title, $subtitle, $accent, $cropTop = 80, $cropBottom = 130) {
    $pair = New-Canvas 1080 1920 "#0D1512"
    $bmp = $pair[0]; $g = $pair[1]
    $grad = New-Object System.Drawing.Drawing2D.LinearGradientBrush(
        (New-Object System.Drawing.Rectangle(0,0,1080,1920)),
        [System.Drawing.ColorTranslator]::FromHtml("#0A100D"),
        [System.Drawing.ColorTranslator]::FromHtml("#173F35"),
        [System.Drawing.Drawing2D.LinearGradientMode]::ForwardDiagonal
    )
    $g.FillRectangle($grad, 0, 0, 1080, 1920); $grad.Dispose()
    FillRound $g -120 1360 1320 640 0 "#0A0F0C"
    DrawHeader $g $title $subtitle $accent
    $shot = LoadShot $file $kind
    DrawPhoneMock $g $shot 170 510 740 1340 $cropTop $cropBottom
    $shot.Dispose()
    SavePng $bmp (Join-Path $OutDir ("phone-screenshot-{0:00}.png" -f $index))
    $g.Dispose()
}
function New-AppIcon {
    $pair = New-Canvas 512 512 "#FFFFFF"
    $bmp = $pair[0]; $g = $pair[1]
    DrawLogo $g 0 0 512
    SavePng $bmp (Join-Path $OutDir "app-icon-512.png")
    $g.Dispose()
}
function New-FeatureGraphic {
    $pair = New-Canvas 1024 500 "#173F35"
    $bmp = $pair[0]; $g = $pair[1]
    $grad = New-Object System.Drawing.Drawing2D.LinearGradientBrush(
        (New-Object System.Drawing.Rectangle(0,0,1024,500)),
        [System.Drawing.ColorTranslator]::FromHtml("#0D1512"),
        [System.Drawing.ColorTranslator]::FromHtml("#20644F"),
        [System.Drawing.Drawing2D.LinearGradientMode]::ForwardDiagonal
    )
    $g.FillRectangle($grad, 0, 0, 1024, 500); $grad.Dispose()
    DrawLogo $g 58 58 116
    DrawText $g "Galeri Takip" 58 200 420 58 44 "#FFFFFF" "Bold"
    DrawText $g "Araç stok, maliyet, kar ve kasa yönetimi tek ekranda." 60 266 480 80 24 "#DDEBE6"
    FillRound $g 60 374 170 46 23 "#5FD0AA"
    DrawText $g "Kasa raporu" 84 382 120 28 19 "#0D1512" "Bold" "Center"
    FillRound $g 248 374 190 46 23 "#F4B860"
    DrawText $g "Kar takibi" 286 382 110 28 19 "#173F35" "Bold" "Center"
    $homeShot = LoadShot "WhatsApp Image 2026-06-29 at 10.03.50 (1).jpeg" "home"
    $report = LoadShot "WhatsApp Image 2026-06-29 at 10.03.49.jpeg" "report"
    DrawPhoneMock $g $homeShot 575 28 190 410 80 330
    DrawPhoneMock $g $report 756 58 190 410 80 330
    $homeShot.Dispose(); $report.Dispose()
    SavePng $bmp (Join-Path $OutDir "feature-graphic-1024x500.png")
    $g.Dispose()
}
function New-TabletFromPhone($index, $file, $kind, $title, $subtitle) {
    $pair = New-Canvas 1200 1920 "#0D1512"
    $bmp = $pair[0]; $g = $pair[1]
    FillRound $g 0 0 1200 1920 0 "#0D1512"
    DrawLogo $g 88 72 104
    DrawText $g $title 88 215 1040 90 60 "#FFFFFF" "Bold"
    DrawText $g $subtitle 88 315 980 70 31 "#D8E3DD"
    FillRound $g 100 430 1000 1320 54 "#101512"
    $shot = LoadShot $file $kind
    DrawPhoneMock $g $shot 275 500 650 1180 80 160
    $shot.Dispose()
    SavePng $bmp (Join-Path $OutDir ("tablet-screenshot-{0:00}.png" -f $index))
    $g.Dispose()
}

New-AppIcon
New-FeatureGraphic
New-StoreShot 1 "WhatsApp Image 2026-06-29 at 10.03.50 (1).jpeg" "home" "Stoktaki araçlarını yönet" "Alış, masraf ve stok maliyetini hızlıca takip et." "#5FD0AA" 80 150
New-StoreShot 2 "WhatsApp Image 2026-06-29 at 10.03.50.jpeg" "detail" "Araç maliyeti her zaman net" "Fotoğraflı araç kartlarıyla masraf ve satış durumunu izle." "#F4B860" 80 360
New-StoreShot 3 "WhatsApp Image 2026-06-29 at 10.03.49.jpeg" "report" "Kar ve zarar raporları hazır" "Satış cirosu, toplam maliyet ve net karı tek bakışta gör." "#5FD0AA" 80 160
New-StoreShot 4 "WhatsApp Image 2026-06-29 at 10.03.49 (1).jpeg" "cash" "Gelir gider kasası cebinde" "Bu ayın tahsilatlarını, giderlerini ve personel ödemelerini izle." "#F4B860" 80 140
New-StoreShot 5 "WhatsApp Image 2026-06-29 at 10.03.48 (2).jpeg" "expense" "Masraf dağılımını analiz et" "Ulaşım, eksper, sigorta ve yakıt giderlerini kategoriye ayır." "#5FD0AA" 80 150
New-StoreShot 6 "WhatsApp Image 2026-06-29 at 10.03.49 (2).jpeg" "wizard" "Araç ekleme akışı kolay" "Alış bilgisi, masraf kalemi ve fotoğrafı adım adım kaydet." "#F4B860" 80 150
New-TabletFromPhone 1 "WhatsApp Image 2026-06-29 at 10.03.50 (1).jpeg" "home" "Galeri yönetimini büyüt" "Stok ve kasa özetlerini geniş ekranda daha rahat takip et."
New-TabletFromPhone 2 "WhatsApp Image 2026-06-29 at 10.03.49.jpeg" "report" "Rapor ekranı her zaman yanında" "Kar, maliyet ve satış cirosunu tablet görünümünde incele."

Compress-Archive -Path (Join-Path $OutDir "*.png") -DestinationPath (Join-Path $OutDir "galeri-takip-play-store-assets.zip") -Force
Write-Host "Generated real Galeri Takip Play Store assets in $OutDir"
