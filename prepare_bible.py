import json, zipfile, xml.etree.ElementTree as ET
from pathlib import Path

names = 'Genesis|Exodus|Leviticus|Numbers|Deuteronomy|Joshua|Judges|Ruth|1 Samuel|2 Samuel|1 Kings|2 Kings|1 Chronicles|2 Chronicles|Ezra|Nehemiah|Esther|Job|Psalms|Proverbs|Ecclesiastes|Song of Solomon|Isaiah|Jeremiah|Lamentations|Ezekiel|Daniel|Hosea|Joel|Amos|Obadiah|Jonah|Micah|Nahum|Habakkuk|Zephaniah|Haggai|Zechariah|Malachi|Matthew|Mark|Luke|John|Acts|Romans|1 Corinthians|2 Corinthians|Galatians|Ephesians|Philippians|Colossians|1 Thessalonians|2 Thessalonians|1 Timothy|2 Timothy|Titus|Philemon|Hebrews|James|1 Peter|2 Peter|1 John|2 John|3 John|Jude|Revelation'.split('|')
root = Path(__file__).parent
z = zipfile.ZipFile(root/'web_vpl.zip')
verses = ET.fromstring(z.read('engwebp_vpl.xml'))
books = []
for v in verses:
    code, c, number = v.attrib['b'], int(v.attrib['c']), v.attrib['v']
    if not books or books[-1]['id'] != code:
        books.append({'id':code,'name':names[len(books)],'chapters':[]})
    chapters=books[-1]['chapters']
    while len(chapters)<c: chapters.append([])
    chapters[c-1].append({'n':number,'t':''.join(v.itertext()).strip()})
assert len(books)==66
assert sum(len(b['chapters']) for b in books)==1189
assert books[-1]['chapters'][-1][-1]['n']=='21'
pack={'format':'abide-bible-1','translation':'World English Bible','abbreviation':'WEB','copyright':'World English Bible, Protestant Edition. Public domain. Source: eBible.org/engwebp. Scripture text is unmodified; formatting and footnotes are omitted in the source verse-per-line edition.','books':books}
(root/'app/src/main/assets/bible.json').write_text(json.dumps(pack,ensure_ascii=False,separators=(',',':')),encoding='utf-8')
print(len(books),'books;',sum(len(b['chapters']) for b in books),'chapters;',len(verses),'verse records')
