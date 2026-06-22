from pathlib import Path
from PIL import Image, ImageDraw, ImageFont, ImageOps


ROOT = Path(__file__).resolve().parents[1]
OUT_DIR = ROOT / "artifacts" / "wireframes"
OUT = OUT_DIR / "pnu_emotion_wireframe_board_with_real_photos.png"


def find_doc_image(name: str) -> Path:
    matches = list((Path.home() / "Documents").rglob(name))
    if not matches:
        raise FileNotFoundError(name)
    return matches[0]


def font(size: int, bold: bool = False) -> ImageFont.FreeTypeFont:
    candidates = [
        Path("C:/Windows/Fonts/malgunbd.ttf" if bold else "C:/Windows/Fonts/malgun.ttf"),
        Path("C:/Windows/Fonts/arialbd.ttf" if bold else "C:/Windows/Fonts/arial.ttf"),
    ]
    for path in candidates:
        if path.exists():
            return ImageFont.truetype(str(path), size=size)
    return ImageFont.load_default()


F = {
    "title": font(42, True),
    "h": font(28, True),
    "sub": font(21, False),
    "small": font(17, False),
    "small_b": font(17, True),
    "tiny": font(14, False),
}


BG = "#f7f7f9"
INK = "#30323a"
MUTED = "#7b8090"
LINE = "#b8b2c7"
ACCENT = "#7c759a"
FILL = "#ffffff"
SOFT = "#eceaf2"
DARK = "#363944"


def rounded(draw, box, r=22, fill=FILL, outline=INK, width=3):
    draw.rounded_rectangle(box, radius=r, fill=fill, outline=outline, width=width)


def paste_round(base, img, box, radius=16):
    x1, y1, x2, y2 = box
    target = (x2 - x1, y2 - y1)
    crop = ImageOps.fit(img.convert("RGB"), target, method=Image.Resampling.LANCZOS)
    mask = Image.new("L", target, 0)
    md = ImageDraw.Draw(mask)
    md.rounded_rectangle((0, 0, target[0], target[1]), radius=radius, fill=255)
    base.paste(crop, (x1, y1), mask)


def crop(img, box):
    return img.crop(box)


def phone(draw, xy, title):
    x, y = xy
    w, h = 320, 680
    rounded(draw, (x, y, x + w, y + h), 34, "#fbfbfc", INK, 4)
    draw.rounded_rectangle((x + 98, y + 12, x + 222, y + 22), radius=6, fill="#d7d7dc")
    draw.text((x + 24, y + 42), title, font=F["h"], fill=INK)
    draw.line((x + 24, y + 82, x + w - 24, y + 82), fill="#ddddE5", width=2)
    return x, y, w, h


def card(draw, box, label=None, fill="#ffffff", outline="#d6d3df", r=18):
    draw.rounded_rectangle(box, radius=r, fill=fill, outline=outline, width=2)
    if label:
        draw.text((box[0] + 14, box[1] + 12), label, font=F["small_b"], fill=INK)


def button(draw, box, label, fill=DARK, fg="#ffffff"):
    draw.rounded_rectangle(box, radius=(box[3] - box[1]) // 2, fill=fill, outline="#c7c4d2", width=2)
    tw = draw.textlength(label, font=F["small_b"])
    draw.text((box[0] + (box[2] - box[0] - tw) / 2, box[1] + 13), label, font=F["small_b"], fill=fg)


def chip(draw, x, y, text, active=False):
    fill = DARK if active else SOFT
    fg = "#ffffff" if active else INK
    w = int(draw.textlength(text, font=F["tiny"])) + 34
    draw.rounded_rectangle((x, y, x + w, y + 36), radius=18, fill=fill, outline="#cbc8d5", width=2)
    draw.text((x + 17, y + 9), text, font=F["tiny"], fill=fg)
    return x + w + 10


def arrow(draw, start, end, label=None):
    draw.line((start, end), fill=ACCENT, width=3)
    ex, ey = end
    sx, sy = start
    if abs(ex - sx) > abs(ey - sy):
        d = 1 if ex > sx else -1
        pts = [(ex, ey), (ex - 14 * d, ey - 8), (ex - 14 * d, ey + 8)]
    else:
        d = 1 if ey > sy else -1
        pts = [(ex, ey), (ex - 8, ey - 14 * d), (ex + 8, ey - 14 * d)]
    draw.polygon(pts, fill=ACCENT)
    if label:
        draw.text(((sx + ex) / 2 + 8, (sy + ey) / 2 - 20), label, font=F["tiny"], fill=ACCENT)


def home(base, draw, xy):
    x, y, w, h = phone(draw, xy, "홈")
    draw.text((x + 24, y + 106), "기쁜데 슬프다", font=F["small_b"], fill=INK)
    chip(draw, x + 210, y + 103, "PNU", False)
    card(draw, (x + 24, y + 150, x + w - 24, y + 250), "과방 채널 선택")
    nx = chip(draw, x + 42, y + 195, "컴공 21", True)
    chip(draw, nx, y + 195, "미공개", False)
    card(draw, (x + 24, y + 275, x + w - 24, y + 435), "오늘의 콤보")
    draw.text((x + 42, y + 322), "기쁨 .7 + 슬픔 .1", font=F["small_b"], fill=INK)
    draw.text((x + 42, y + 354), "+ 분노 .1 + 놀람 .1", font=F["small_b"], fill=INK)
    button(draw, (x + 44, y + 382, x + w - 44, y + 422), "셀카 촬영", "#ded3c8", INK)
    card(draw, (x + 24, y + 460, x + 150, y + 575), "대학 피드")
    card(draw, (x + 170, y + 460, x + w - 24, y + 575), "아카이브")


def menu(draw, xy):
    x, y, w, h = phone(draw, xy, "메뉴")
    draw.rectangle((x + 4, y + 90, x + w - 4, y + h - 4), fill="#fdfdfd")
    draw.text((x + 30, y + 125), "기쁜데 슬프다", font=F["h"], fill=INK)
    draw.text((x + 30, y + 166), "PNU 감정 셀카 미션", font=F["small"], fill=MUTED)
    draw.text((x + 30, y + 215), "현재 채널 · 컴공 21", font=F["small_b"], fill=INK)
    draw.line((x + 4, y + 260, x + w - 4, y + 260), fill="#d7d4df", width=2)
    draw.text((x + 55, y + 318), "대학 피드", font=F["h"], fill=INK)
    draw.text((x + 55, y + 408), "아카이브", font=F["h"], fill=INK)


def group_feed(base, draw, xy, photos):
    x, y, w, h = phone(draw, xy, "그룹 피드")
    chip(draw, x + 24, y + 105, "컴공 21", True)
    chip(draw, x + 130, y + 105, "미공개", False)
    draw.text((x + 34, y + 156), "실시간 피드", font=F["small_b"], fill=INK)
    draw.text((x + 184, y + 156), "라이브 랭킹", font=F["small_b"], fill=MUTED)
    draw.line((x + 24, y + 190, x + w - 24, y + 190), fill=ACCENT, width=4)
    card(draw, (x + 24, y + 210, x + w - 24, y + 260), None)
    draw.text((x + 42, y + 225), "초대코드 PNUCS1 · 멤버 4명", font=F["tiny"], fill=INK)
    draw.rounded_rectangle((x + 24, y + 285, x + w - 24, y + 645), radius=24, fill="#3a3d48", outline="#9c9bb5", width=3)
    draw.text((x + 42, y + 307), "감정 캐치", font=F["small_b"], fill="#ffffff")
    labels = [("과음(나)", "91pt"), ("호날두", "60pt"), ("카리나", "78pt"), ("강동원", "73pt")]
    spots = [(x + 42, y + 350), (x + 166, y + 350), (x + 42, y + 495), (x + 166, y + 495)]
    for i, ((name, score), (px, py)) in enumerate(zip(labels, spots)):
        draw.rounded_rectangle((px, py, px + 110, py + 125), radius=13, fill="#46505a", outline="#85849a", width=2)
        draw.text((px + 8, py + 8), name, font=F["tiny"], fill="#ffffff")
        draw.text((px + 70, py + 8), score, font=F["tiny"], fill="#d9e1be")
        paste_round(base, photos[i], (px + 8, py + 34, px + 102, py + 95), radius=8)
        draw.text((px + 8, py + 100), "기쁨", font=F["tiny"], fill="#d9e1be")


def ranking(draw, xy):
    x, y, w, h = phone(draw, xy, "라이브 랭킹")
    chip(draw, x + 24, y + 105, "컴공 21", True)
    draw.text((x + 34, y + 156), "실시간 피드", font=F["small_b"], fill=MUTED)
    draw.text((x + 174, y + 156), "라이브 랭킹", font=F["small_b"], fill=INK)
    draw.line((x + 170, y + 190, x + w - 24, y + 190), fill=ACCENT, width=4)
    draw.text((x + 34, y + 225), "오늘의 감정 챔피언", font=F["small_b"], fill=INK)
    scores = ["91.6점", "89.1점", "80.4점", "78.7점", "73.9점"]
    for i, score in enumerate(scores):
        yy = y + 270 + i * 72
        card(draw, (x + 24, yy, x + w - 24, yy + 56), None)
        draw.ellipse((x + 42, yy + 13, x + 72, yy + 43), fill=SOFT, outline=ACCENT, width=2)
        draw.text((x + 52, yy + 18), str(i + 1), font=F["tiny"], fill=INK)
        draw.ellipse((x + 86, yy + 12, x + 120, yy + 46), fill="#d8dce7")
        draw.text((x + 132, yy + 12), "부산대 멤버", font=F["small_b"], fill=INK)
        draw.text((x + 132, yy + 35), "일치 점수", font=F["tiny"], fill=MUTED)
        draw.text((x + 236, yy + 20), score, font=F["small_b"], fill=INK)


def mission(base, draw, xy, selfie):
    x, y, w, h = phone(draw, xy, "미션 촬영")
    draw.rounded_rectangle((x + 24, y + 105, x + w - 24, y + 185), radius=18, fill=DARK, outline="#aaa5ba", width=2)
    draw.text((x + 42, y + 125), "목표 감정 조합", font=F["tiny"], fill="#c8d3b2")
    draw.text((x + 42, y + 150), "기쁨 .7 + 슬픔 .1", font=F["small_b"], fill="#ffffff")
    paste_round(base, selfie, (x + 24, y + 210, x + w - 24, y + 425), radius=18)
    for dx, dy in [(38, 224), (w - 72, 224), (38, 386), (w - 72, 386)]:
        draw.rectangle((x + dx, y + dy, x + dx + 28, y + dy + 28), outline=INK, width=5)
    button(draw, (x + 26, y + 450, x + 150, y + 492), "카메라", "#eee4db", INK)
    button(draw, (x + 170, y + 450, x + w - 26, y + 492), "갤러리", "#eee4db", INK)
    draw.text((x + 24, y + 525), "프리셋", font=F["small_b"], fill=INK)
    for i in range(4):
        xx = x + 24 + (i % 2) * 140
        yy = y + 558 + (i // 2) * 48
        card(draw, (xx, yy, xx + 125, yy + 38), "표정 샘플", "#fbf8f5")
    button(draw, (x + 24, y + 630, x + w - 24, y + 670), "표정 채점하기", "#69886b", "#ffffff")


def face_wait(draw, xy):
    x, y, w, h = phone(draw, xy, "얼굴 검증 대기")
    draw.rounded_rectangle((x + 24, y + 105, x + w - 24, y + 185), radius=18, fill=DARK, outline="#aaa5ba", width=2)
    draw.text((x + 42, y + 140), "목표 감정 조합", font=F["small_b"], fill="#ffffff")
    card(draw, (x + 24, y + 210, x + w - 24, y + 425), None, "#ffffff")
    draw.rectangle((x + 60, y + 250, x + w - 60, y + 385), outline="#bfc0ca", width=2)
    draw.text((x + 82, y + 304), "얼굴 1명만 감지", font=F["small_b"], fill=MUTED)
    button(draw, (x + 26, y + 450, x + 150, y + 492), "카메라", "#eee4db", INK)
    button(draw, (x + 170, y + 450, x + w - 26, y + 492), "갤러리", "#eee4db", INK)
    button(draw, (x + 24, y + 630, x + w - 24, y + 670), "검증 대기", "#8792a5", "#ffffff")


def result(base, draw, xy, selfie, map_img):
    x, y, w, h = phone(draw, xy, "분석 결과")
    card(draw, (x + 24, y + 105, x + w - 24, y + 205), None)
    draw.text((x + 92, y + 122), "최종 분석 스코어", font=F["small"], fill=MUTED)
    draw.text((x + 102, y + 148), "91.6점", font=F["title"], fill=INK)
    paste_round(base, selfie, (x + 24, y + 230, x + 145, y + 370), radius=18)
    card(draw, (x + 160, y + 230, x + w - 24, y + 370), "감정 벡터 비교")
    for i, (lab, val) in enumerate([("기쁨", 0.73), ("슬픔", 0.14), ("분노", 0.03), ("놀람", 0.08)]):
        yy = y + 270 + i * 22
        draw.text((x + 174, yy), lab, font=F["tiny"], fill=INK)
        draw.line((x + 220, yy + 7, x + 290, yy + 7), fill="#e7e7e7", width=6)
        draw.line((x + 220, yy + 7, x + 220 + int(70 * val), yy + 7), fill="#7b967e", width=6)
    card(draw, (x + 24, y + 395, x + w - 24, y + 590), "위치 태그")
    paste_round(base, map_img, (x + 42, y + 430, x + w - 42, y + 535), radius=10)
    chip(draw, x + 42, y + 548, "정문", False)
    chip(draw, x + 100, y + 548, "정보컴", False)
    chip(draw, x + 190, y + 548, "넉넉한터", False)
    button(draw, (x + 24, y + 620, x + 142, y + 662), "다시 촬영", "#eee4db", INK)
    button(draw, (x + 154, y + 620, x + w - 24, y + 662), "피드 올리기", "#69886b", "#ffffff")


def archive(base, draw, xy, slide_img):
    x, y, w, h = phone(draw, xy, "아카이브")
    draw.text((x + 24, y + 110), "추억 슬라이드쇼", font=F["small_b"], fill=MUTED)
    card(draw, (x + 24, y + 150, x + w - 24, y + 455), None)
    draw.ellipse((x + 44, y + 170, x + 78, y + 204), fill=SOFT)
    draw.text((x + 92, y + 170), "나_부산대컴공", font=F["small_b"], fill=INK)
    draw.text((x + 92, y + 194), "2026-06-21", font=F["tiny"], fill=MUTED)
    paste_round(base, slide_img, (x + 24, y + 220, x + w - 24, y + 370), radius=0)
    draw.rounded_rectangle((x + 240, y + 232, x + 288, y + 260), radius=6, fill=DARK)
    draw.text((x + 252, y + 236), "2/6", font=F["tiny"], fill="#ffffff")
    draw.text((x + 42, y + 390), "미션 감정: HAPPY .7 + SAD .1", font=F["tiny"], fill=MUTED)
    draw.text((x + 42, y + 418), "실제 표정: HAPPY .7 + SURPRISED .2", font=F["tiny"], fill=INK)
    card(draw, (x + 24, y + 485, x + w - 24, y + 650), "자동 슬라이드쇼")
    draw.text((x + 42, y + 530), "전환 간격: 2.0초", font=F["tiny"], fill=MUTED)
    draw.line((x + 42, y + 565, x + w - 42, y + 565), fill="#d6d6d6", width=7)
    draw.line((x + 42, y + 565, x + 120, y + 565), fill=ACCENT, width=7)
    button(draw, (x + 42, y + 590, x + 148, y + 632), "이전 장", "#eee4db", INK)
    button(draw, (x + 170, y + 590, x + w - 42, y + 632), "다음 장", "#eee4db", INK)


def modal(draw, xy):
    x, y, w, h = phone(draw, xy, "채널 모달")
    card(draw, (x + 24, y + 115, x + w - 24, y + 335), None, "#f4f3fa")
    draw.text((x + 44, y + 140), "과방 참여", font=F["h"], fill=INK)
    draw.text((x + 44, y + 182), "6자리 초대코드 입력", font=F["small"], fill=MUTED)
    draw.rounded_rectangle((x + 44, y + 220, x + w - 44, y + 265), radius=8, fill="#ffffff", outline=MUTED, width=2)
    draw.text((x + 58, y + 233), "초대코드", font=F["small"], fill=MUTED)
    button(draw, (x + 164, y + 285, x + w - 44, y + 325), "입장받기", DARK, "#ffffff")
    card(draw, (x + 24, y + 380, x + w - 24, y + 640), None, "#f4f3fa")
    draw.text((x + 44, y + 405), "과방 개설", font=F["h"], fill=INK)
    for i, lab in enumerate(["배틀방 이름", "입장코드"]):
        yy = y + 455 + i * 60
        draw.rounded_rectangle((x + 44, yy, x + w - 44, yy + 45), radius=8, fill="#ffffff", outline=MUTED, width=2)
        draw.text((x + 58, yy + 13), lab, font=F["small"], fill=MUTED)
    button(draw, (x + 164, y + 590, x + w - 44, y + 630), "개설", DARK, "#ffffff")


def main():
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    img5 = Image.open(find_doc_image("KakaoTalk_20260621_204401878_05.png"))
    img7 = Image.open(find_doc_image("KakaoTalk_20260621_204401878_07.png"))
    img6 = Image.open(find_doc_image("KakaoTalk_20260621_204401878_06.png"))
    img3 = Image.open(find_doc_image("KakaoTalk_20260621_204401878_03.png"))

    feed_photos = [
        crop(img5, (110, 1110, 500, 1335)),
        crop(img5, (580, 1110, 970, 1335)),
        crop(img5, (110, 1535, 500, 1765)),
        crop(img5, (580, 1535, 970, 1765)),
    ]
    mission_selfie = crop(img7, (50, 445, 1030, 1160))
    result_selfie = crop(img6, (55, 760, 430, 1200))
    map_img = crop(img6, (70, 1370, 1010, 1790))
    archive_img = crop(img3, (48, 510, 1030, 1210))

    base = Image.new("RGB", (3600, 2400), BG)
    draw = ImageDraw.Draw(base)
    draw.text((110, 58), "기쁜데 슬프다 Android Wireflow", font=F["title"], fill=INK)
    draw.text((112, 112), "실제 사진 포함 · low-fidelity wireframe · 코드 화면 구조 반영", font=F["sub"], fill=MUTED)

    pos = {
        "home": (170, 185),
        "menu": (650, 185),
        "feed": (1130, 185),
        "rank": (1610, 185),
        "modal": (2090, 185),
        "mission": (170, 980),
        "wait": (650, 980),
        "result": (1130, 980),
        "archive": (1610, 980),
    }

    home(base, draw, pos["home"])
    menu(draw, pos["menu"])
    group_feed(base, draw, pos["feed"], feed_photos)
    ranking(draw, pos["rank"])
    modal(draw, pos["modal"])
    mission(base, draw, pos["mission"], mission_selfie)
    face_wait(draw, pos["wait"])
    result(base, draw, pos["result"], result_selfie, map_img)
    archive(base, draw, pos["archive"], archive_img)

    def center_right(key):
        x, y = pos[key]
        return x + 320, y + 340

    def center_left(key):
        x, y = pos[key]
        return x, y + 340

    def center_bottom(key):
        x, y = pos[key]
        return x + 160, y + 680

    def center_top(key):
        x, y = pos[key]
        return x + 160, y

    arrow(draw, center_right("home"), center_left("menu"), "메뉴")
    arrow(draw, center_right("menu"), center_left("feed"), "피드")
    arrow(draw, center_right("feed"), center_left("rank"), "탭")
    arrow(draw, center_right("rank"), center_left("modal"), "참여/개설")
    arrow(draw, center_bottom("home"), center_top("mission"), "셀카 촬영")
    arrow(draw, center_right("mission"), center_left("wait"), "얼굴 검증")
    arrow(draw, center_right("wait"), center_left("result"), "TFLite 분석")
    arrow(draw, center_top("archive"), (pos["feed"][0] + 160, pos["feed"][1] + 680), "아카이브")
    arrow(draw, (pos["result"][0] + 160, pos["result"][1]), (pos["feed"][0] + 160, pos["feed"][1] + 680), "업로드")

    base.save(OUT)
    print(OUT)


if __name__ == "__main__":
    main()
