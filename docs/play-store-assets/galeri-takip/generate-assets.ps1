Add-Type -AssemblyName System.Drawing

$ErrorActionPreference = "Stop"
$OutDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$LogoPath = Join-Path $OutDir "stitch-galeri-takip-icon.png"

function New-Canvas($w, $h, $color) {
    $bmp = New-Object System.Drawing.Bitmap($w, $h, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $g.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::AntiAliasGridFit
    $g.Clear([System.Drawing.ColorTranslator]::FromHtml($color))
    return @($bmp, $g)
}

function Brush($hex) { New-Object System.Drawing.SolidBrush([System.Drawing.ColorTranslator]::FromHtml($hex)) }
function Pen($hex, $width = 1) { New-Object System.Drawing.Pen([System.Drawing.ColorTranslator]::FromHtml($hex), $width) }
function Font($size, $style = "Regular") {
    $fontStyle = [System.Drawing.FontStyle]::Regular
    if ($style -eq "Bold") { $fontStyle = [System.Drawing.FontStyle]::Bold }
    New-Object System.Drawing.Font("Segoe UI", $size, $fontStyle, [System.Drawing.GraphicsUnit]::Pixel)
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

function DrawLogoMark($g, $x, $y, $s) {
    if (Test-Path $LogoPath) {
        $logo = [System.Drawing.Image]::FromFile($LogoPath)
        $dst = New-Object System.Drawing.Rectangle($x, $y, $s, $s)
        $g.DrawImage($logo, $dst)
        $logo.Dispose()
        return
    }
    FillRound $g $x $y $s $s ([int]($s*.22)) "#173F35"
    DrawCar $g ($x + [int]($s*.08)) ($y + [int]($s*.11)) ([int]($s*.84))
}

function New-AppIcon {
    $pair = New-Canvas 512 512 "#FFFFFF"
    $bmp = $pair[0]; $g = $pair[1]
    DrawLogoMark $g 0 0 512
    SavePng $bmp (Join-Path $OutDir "app-icon-512.png")
    $g.Dispose()
}

function DrawPhone($g, $x, $y, $w, $h, $screenTitle, $accent) {
    FillRound $g $x $y $w $h 54 "#14231F"
    FillRound $g ($x+18) ($y+24) ($w-36) ($h-48) 42 "#F7F6F2"
    DrawText $g "Galeri Takip" ($x+42) ($y+58) ($w-84) 46 22 "#173F35" "Bold"
    FillRound $g ($x+42) ($y+122) ($w-84) 112 22 $accent
    DrawText $g $screenTitle ($x+62) ($y+143) ($w-124) 34 21 "#FFFFFF" "Bold"
    DrawText $g "₺2.840.000" ($x+62) ($y+178) 230 42 30 "#FFFFFF" "Bold"
    FillRound $g ($x+42) ($y+264) ($w-84) 104 18 "#FFFFFF"
    StrokeRound $g ($x+42) ($y+264) ($w-84) 104 18 "#E2E2DC" 2
    DrawCar $g ($x+62) ($y+286) 74
    DrawText $g "BMW 320i" ($x+155) ($y+284) 180 30 18 "#1E2623" "Bold"
    DrawText $g "34 ABC 123" ($x+155) ($y+315) 190 26 14 "#69716D"
    DrawText $g "+₺185.000" ($x+$w-145) ($y+302) 90 28 16 "#188159" "Bold" "Far"
    FillRound $g ($x+42) ($y+390) ($w-84) 104 18 "#FFFFFF"
    StrokeRound $g ($x+42) ($y+390) ($w-84) 104 18 "#E2E2DC" 2
    DrawCar $g ($x+62) ($y+412) 74
    DrawText $g "Audi A4" ($x+155) ($y+410) 180 30 18 "#1E2623" "Bold"
    DrawText $g "Stokta" ($x+155) ($y+441) 190 26 14 "#69716D"
    DrawText $g "₺1.420.000" ($x+$w-158) ($y+428) 110 28 16 "#173F35" "Bold" "Far"
    FillRound $g ($x+42) ($y+526) ($w-84) 126 20 "#FFF4E1"
    DrawText $g "Kasa Özeti" ($x+62) ($y+550) 180 28 18 "#8F5B0D" "Bold"
    DrawText $g "Gelir ₺420.000`nGider ₺86.500" ($x+62) ($y+584) 230 48 15 "#1E2623"
}

function New-FeatureGraphic {
    $pair = New-Canvas 1024 500 "#173F35"
    $bmp = $pair[0]; $g = $pair[1]
    FillRound $g 0 0 1024 500 0 "#173F35"
    FillRound $g 52 62 120 120 26 "#FFF9F0"
    DrawCar $g 68 86 88
    DrawText $g "Galeri Takip" 210 74 430 60 46 "#FFFFFF" "Bold"
    DrawText $g "Araç stok, maliyet, kar ve kasa takibi tek uygulamada." 212 148 500 92 26 "#DDEBE6"
    FillRound $g 212 276 190 52 26 "#F4B860"
    DrawText $g "Araç maliyeti" 240 287 140 32 20 "#173F35" "Bold"
    FillRound $g 422 276 170 52 26 "#E0ECE7"
    DrawText $g "Kasa raporu" 450 287 125 32 20 "#173F35" "Bold"
    DrawPhone $g 696 24 250 452 "Stok Değeri" "#173F35"
    SavePng $bmp (Join-Path $OutDir "feature-graphic-1024x500.png")
    $g.Dispose()
}

function New-PhoneShot($index, $title, $subtitle, $screenTitle, $accent) {
    $pair = New-Canvas 1080 1920 "#F7F6F2"
    $bmp = $pair[0]; $g = $pair[1]
    DrawLogoMark $g 74 72 116
    DrawText $g $title 74 230 920 120 56 "#1E2623" "Bold"
    DrawText $g $subtitle 74 360 900 92 30 "#69716D"
    DrawPhone $g 150 530 780 1200 $screenTitle $accent
    DrawText $g "Galeri Takip" 74 1810 360 42 28 "#173F35" "Bold"
    SavePng $bmp (Join-Path $OutDir ("phone-screenshot-{0:00}.png" -f $index))
    $g.Dispose()
}

function New-TabletShot($index, $title, $subtitle) {
    $pair = New-Canvas 1200 1920 "#F7F6F2"
    $bmp = $pair[0]; $g = $pair[1]
    DrawText $g $title 88 90 1040 88 54 "#1E2623" "Bold"
    DrawText $g $subtitle 88 178 1000 74 30 "#69716D"
    FillRound $g 90 310 1020 1320 42 "#FFFFFF"
    StrokeRound $g 90 310 1020 1320 42 "#E2E2DC" 4
    DrawText $g "Galeri Takip" 150 372 420 48 34 "#173F35" "Bold"
    FillRound $g 150 460 430 178 28 "#173F35"
    FillRound $g 620 460 430 178 28 "#F4B860"
    DrawText $g "Stok değeri" 190 496 280 34 28 "#DDEBE6"
    DrawText $g "₺8.240.000" 190 540 330 62 44 "#FFFFFF" "Bold"
    DrawText $g "Bu ay kasa" 660 496 280 34 28 "#5A3900"
    DrawText $g "+₺333.500" 660 540 330 62 44 "#173F35" "Bold"
    for ($i=0; $i -lt 5; $i++) {
        $y = 718 + ($i * 145)
        FillRound $g 150 $y 900 108 24 "#FFFFFF"
        StrokeRound $g 150 $y 900 108 24 "#E2E2DC" 3
        DrawCar $g 182 ($y+22) 72
        DrawText $g @("BMW 320i","Mercedes C200","Audi A4","Volkswagen Passat","Toyota Corolla")[$i] 280 ($y+20) 420 34 26 "#1E2623" "Bold"
        DrawText $g @("Satıldı • Kar ₺185.000","Stokta • Toplam maliyet","Stokta","Satıldı • Kar ₺92.000","Stokta")[$i] 280 ($y+58) 430 30 21 "#69716D"
        DrawText $g @("₺1.650.000","₺2.120.000","₺1.420.000","₺1.310.000","₺980.000")[$i] 840 ($y+39) 170 34 24 "#173F35" "Bold" "Far"
    }
    SavePng $bmp (Join-Path $OutDir ("tablet-screenshot-{0:00}.png" -f $index))
    $g.Dispose()
}

New-AppIcon
New-FeatureGraphic
New-PhoneShot 1 "Araçlarını tek ekranda yönet" "Stok, alış fiyatı, satış bilgisi ve kar durumunu hızlıca takip et." "Stok Değeri" "#173F35"
New-PhoneShot 2 "Her aracın maliyeti net olsun" "Masraf kalemlerini araca bağla, gerçek karı gör." "Toplam Maliyet" "#8F5B0D"
New-PhoneShot 3 "Kasa gelir gider takibi" "Bu ay ve geçen ay işletmende ne olduğunu kolayca izle." "Kasa Özeti" "#188159"
New-PhoneShot 4 "Kategori bazlı raporlar" "Gelir, gider ve personel ödemelerini tiplerine göre incele." "Raporlar" "#173F35"
New-PhoneShot 5 "Fotoğraflı araç kaydı" "Araç fotoğraflarını ekle, düzenle ve gerektiğinde sil." "Araç Detayı" "#8F5B0D"
New-PhoneShot 6 "Bulut yedekleme desteği" "Google ile giriş yap, kayıtlarını Firebase ile koru." "Bulut Yedek" "#188159"
New-TabletShot 1 "Galeri yönetimi geniş ekranda" "Araç stoku, maliyet ve kasa özetlerini tablet görünümünde rahatça incele."
New-TabletShot 2 "Raporlarını daha net gör" "Gelir, gider ve kar takibini geniş ekranda düzenli görüntüle."

Compress-Archive -Path (Join-Path $OutDir "*.png") -DestinationPath (Join-Path $OutDir "galeri-takip-play-store-assets.zip") -Force
Write-Host "Generated Galeri Takip Play Store assets in $OutDir"
