import React from 'react';
import { View, StyleSheet, ScrollView } from 'react-native';
import {
  Title,
  Card,
  Button,
  List,
  Text,
  Divider,
} from 'react-native-paper';
import { useAuth } from '../context/AuthContext';

export default function ProfileScreen() {
  const { user, logout } = useAuth();

  const handleLogout = async () => {
    await logout();
  };

  return (
    <ScrollView style={styles.container}>
      <View style={styles.content}>
        <Card style={styles.card}>
          <Card.Content>
            <View style={styles.header}>
              <View style={styles.avatar}>
                <Text style={styles.avatarText}>
                  {user?.name?.charAt(0).toUpperCase()}
                </Text>
              </View>
              <Title style={styles.name}>{user?.name}</Title>
              <Text style={styles.email}>{user?.email}</Text>
            </View>
          </Card.Content>
        </Card>

        <Card style={styles.card}>
          <Card.Content>
            <Title>Profile Information</Title>
            <Divider style={styles.divider} />

            <List.Item
              title="Email"
              description={user?.email}
              left={(props) => <List.Icon {...props} icon="email" />}
            />

            {user?.roomNumber && (
              <List.Item
                title="Room Number"
                description={user.roomNumber}
                left={(props) => <List.Icon {...props} icon="door" />}
              />
            )}

            {user?.dormName && (
              <List.Item
                title="Dorm"
                description={user.dormName}
                left={(props) => <List.Icon {...props} icon="home" />}
              />
            )}

            <List.Item
              title="Roommates"
              description={`${user?.roommates?.length || 0} connected`}
              left={(props) => <List.Icon {...props} icon="account-group" />}
            />
          </Card.Content>
        </Card>

        <Card style={styles.card}>
          <Card.Content>
            <Title>About</Title>
            <Divider style={styles.divider} />
            <Text style={styles.aboutText}>
              Sock on the Door is a simple way to communicate your availability
              with your roommates. Set your status and let them know when you're
              busy, available, or need privacy.
            </Text>
          </Card.Content>
        </Card>

        <Button
          mode="outlined"
          onPress={handleLogout}
          style={styles.logoutButton}
          icon="logout"
        >
          Sign Out
        </Button>
      </View>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f5f5f5',
  },
  content: {
    padding: 16,
  },
  card: {
    marginBottom: 16,
  },
  header: {
    alignItems: 'center',
    paddingVertical: 16,
  },
  avatar: {
    width: 80,
    height: 80,
    borderRadius: 40,
    backgroundColor: '#6200ee',
    justifyContent: 'center',
    alignItems: 'center',
    marginBottom: 16,
  },
  avatarText: {
    fontSize: 32,
    color: 'white',
    fontWeight: 'bold',
  },
  name: {
    fontSize: 24,
    marginBottom: 4,
  },
  email: {
    fontSize: 14,
    color: '#666',
  },
  divider: {
    marginVertical: 16,
  },
  aboutText: {
    lineHeight: 22,
    color: '#666',
  },
  logoutButton: {
    marginVertical: 16,
  },
});
