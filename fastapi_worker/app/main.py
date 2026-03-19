import logging

# 로그 형식
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s | %(levelname)-8s | %(filename)s | %(message)s",
    datefmt="%Y-%m-%d %H:%M:%S"
)