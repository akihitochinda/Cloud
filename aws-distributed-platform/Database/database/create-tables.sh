#!/bin/bash

set -e

echo "Creating SwipeEvents..."

aws dynamodb create-table \
 --table-name SwipeEvents \
 --attribute-definitions \
 AttributeName=SkierId,AttributeType=S \
 AttributeName=EventTimestamp,AttributeType=S \
 --key-schema \
 AttributeName=SkierId,KeyType=HASH \
 AttributeName=EventTimestamp,KeyType=RANGE \
 --billing-mode PAY_PER_REQUEST

echo "Creating SkierStats..."

aws dynamodb create-table \
 --table-name SkierStats \
 --attribute-definitions \
 AttributeName=SkierId,AttributeType=S \
 --key-schema \
 AttributeName=SkierId,KeyType=HASH \
 --billing-mode PAY_PER_REQUEST

echo "Creating SkierLiftStats..."

aws dynamodb create-table \
 --table-name SkierLiftStats \
 --attribute-definitions \
 AttributeName=SkierId,AttributeType=S \
 AttributeName=LiftId,AttributeType=S \
 --key-schema \
 AttributeName=SkierId,KeyType=HASH \
 AttributeName=LiftId,KeyType=RANGE \
 --billing-mode PAY_PER_REQUEST

echo "Creating SkierResortDayStats..."

aws dynamodb create-table \
 --table-name SkierResortDayStats \
 --attribute-definitions \
 AttributeName=SkierId,AttributeType=S \
 AttributeName=ResortDay,AttributeType=S \
 --key-schema \
 AttributeName=SkierId,KeyType=HASH \
 AttributeName=ResortDay,KeyType=RANGE \
 --billing-mode PAY_PER_REQUEST

echo "Creating LeaderboardSnapshot..."

aws dynamodb create-table \
 --table-name LeaderboardSnapshot \
 --attribute-definitions \
 AttributeName=ResortDay,AttributeType=S \
 AttributeName=Rank,AttributeType=N \
 --key-schema \
 AttributeName=ResortDay,KeyType=HASH \
 AttributeName=Rank,KeyType=RANGE \
 --billing-mode PAY_PER_REQUEST

echo "Creating ActivityWindow..."

aws dynamodb create-table \
 --table-name ActivityWindow \
 --attribute-definitions \
 AttributeName=MinuteBucket,AttributeType=S \
 AttributeName=SkierId,AttributeType=S \
 --key-schema \
 AttributeName=MinuteBucket,KeyType=HASH \
 AttributeName=SkierId,KeyType=RANGE \
 --billing-mode PAY_PER_REQUEST

echo "Creating TrafficMetrics..."

aws dynamodb create-table \
 --table-name TrafficMetrics \
 --attribute-definitions \
 AttributeName=TimeBucket,AttributeType=S \
 --key-schema \
 AttributeName=TimeBucket,KeyType=HASH \
 --billing-mode PAY_PER_REQUEST

aws dynamodb list-tables