#!/bin/bash
# Install RabbitMQ on Amazon Linux 2023
sudo dnf update -y
sudo dnf install -y rabbitmq-server
sudo systemctl enable rabbitmq-server
sudo systemctl start rabbitmq-server
sudo systemctl status rabbitmq-server --no-pager
echo "RabbitMQ setup complete"
